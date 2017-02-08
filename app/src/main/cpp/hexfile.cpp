/**********************************************
**    This file is part of Lorris
**    http://tasssadar.github.com/Lorris/
**
**    See README and COPYING
***********************************************/

#include <stdlib.h>
#include <algorithm>

#include "hexfile.h"
#include "chipdefs.h"

// Most of this file is ported from avr232client, file program.hpp

void HexFile::Patcher::patchPage(page &p)
{
    // FIXME: should be assert
    if(p.data.empty())
        return;

    if(m_patch_pos == 0)
        return;

    if(p.address == 0)
    {
        m_entrypt_jmp = (m_boot_reset /2 - 1) | 0xC000;
        if((m_entrypt_jmp & 0xF000) != 0xC000)
            throw std::string("Cannot patch the program, it does not begin with rjmp instruction.");
        p.data[0] = (uint8_t)m_entrypt_jmp;
        p.data[1] = (uint8_t)(m_entrypt_jmp >> 8);
        return;
    }

    if(p.address > m_patch_pos || p.address + p.data.size() <= m_patch_pos)
        return;

    uint32_t new_patch_pos = m_patch_pos - p.address;

    if(p.data[new_patch_pos] != 0xFF || p.data[new_patch_pos + 1] != 0xFF)
        throw std::string("The program is incompatible with this patching algorithm.");

    uint16_t entry_addr = (m_entrypt_jmp & 0x0FFF) + 1;
    uint16_t patched_instr = ((entry_addr - m_patch_pos / 2 - 1) & 0xFFF) | 0xC000;
    p.data[new_patch_pos] = (uint8_t)patched_instr;
    p.data[new_patch_pos + 1] = (uint8_t)(patched_instr >> 8);
}

HexFile::HexFile()
{
}

inline static int h(char c)
{
    static const char *hex = "0123456789ABCDEF";
    for(uint8_t i = 0; i < 16; ++i)
        if(hex[i] == c)
            return i;
    throw false;
}

void HexFile::LoadFromFile(const std::string &path)
{
    FILE *file = fopen(path.c_str(), "r");
    if(!file)
        throw std::string("Can't open file!");

    clear();

    int base = 0;
    std::vector<uint8_t> rec_nums;
    bool ok;

    char line[128];
    int len;
    for(int lineno = 0; fgets(line, sizeof(line), file); ++lineno)
    {
        len = strlen(line);
        for(int y = 1; y <= 2; ++y)
        {
            if(line[len-1] != '\r' && line[len-1] != '\n')
                break;
            
            line[len-1] = 0;
            --len;
        }

        if(line[0] != ':' || len%2 != 1)
        {
            char buff[64];
            sprintf(buff, "Invalid line format (line %d)", lineno);
            fclose(file);
            throw std::string(buff);
        }

        rec_nums.clear();
        uint8_t checksum = 0;
        for(uint8_t i = 1; true;)
        {
            uint8_t num;
            try
            {
                num= h(line[i++]) << 4;
                num |= h(line[i++]);
            }
            catch(bool b)
            {
                char buff[64];
                sprintf(buff, "Failed to parse hex num (line %d)", lineno);
                fclose(file);
                throw std::string(buff);
            }

            rec_nums.push_back(num);

            if(i < len)
                checksum += num;
            else
                break;
        }
        checksum = 256 - checksum;

        if(checksum != rec_nums[rec_nums.size()-1])
        {
            char buff[64];
            sprintf(buff, "Checksums do not match (line %d)", lineno);
            fclose(file);
            throw std::string(buff);
        }

        int length = rec_nums[0];
        int address = rec_nums[1] * 0x100 + rec_nums[2];
        int rectype = rec_nums[3];

        if (length != (int)rec_nums.size() - 5)
        {
            char buff[64];
            sprintf(buff, "Invalid record lenght specified (line %d)", lineno);
            fclose(file);
            throw std::string(buff);
        }

        switch(rectype)
        {
            case 0: // Data record -- fallthrough to continue
                addRegion(base + address, rec_nums.begin() + 4, rec_nums.begin() + rec_nums.size() - 1, lineno);
                break;
            case 1: // EOF
                fclose(file);
                return;
            case 2: // Extended Segment Address Record
            case 4: // Extended Linear Address Record
            {
                if (length != 2)
                {
                    char buff[64];
                    sprintf(buff, "Invalid type %d record (line %d)", rectype, lineno);
                    fclose(file);
                    throw std::string(buff);
                }
                base = (rec_nums[4] * 0x100 + rec_nums[5]);
                base = (rectype == 2) ? (base * 16) : (base << 16);
                continue;
            }
            case 3: // Start Segment Address Record - unused
                continue;
            default:
            {
                char buff[64];
                sprintf(buff, "Invalid record type %d (line %d)", rectype, lineno);
                fclose(file);
                throw std::string(buff);
            }
        }
    }
    fclose(file);
}

//void add_region(std::size_t pos, byte_type const * first, byte_type const * last, int lineno)
//program.hpp
void HexFile::addRegion(uint32_t pos, std::vector<uint8_t>::iterator first, std::vector<uint8_t>::iterator last, int lineno)
{
    regionMap::iterator itr = m_data.upper_bound(pos);
    if(itr != m_data.begin())
    {
        regionMap::iterator itr2 = itr;
        --itr2;

        if(itr2->first + itr2->second.size() == pos)
        {
            itr2->second.insert(itr2->second.end(), first, last);
            return;
        }

        if(itr2->first + itr2->second.size() > pos)
        {
            char buff[64];
            sprintf(buff, "Memory location was defined twice (line %d)", lineno);
            throw std::string(buff);
        }
    }

    if(itr != m_data.end() && itr->first < pos + (last - first))
    {
        char buff[64];
        sprintf(buff, "Memory location was defined twice (line %d)", lineno);
        throw std::string(buff);
    }

    m_data[pos] = std::vector<uint8_t>(first, last);
}

uint32_t HexFile::getProgSize() const
{   
    uint32_t res = 0;
    for(regionMap::const_iterator itr = m_data.begin(); itr != m_data.end(); ++itr)
        res = std::max(res, uint32_t(itr->first + itr->second.size()));
    return res;
}

/*
void HexFile::SaveToFile(const std::string &path)
{
    QFile file(path);
    if(!file.open(QIODevice::WriteOnly))
        throw std::string(QObject::tr("Can't open file \"%1\"!")).arg(path);

    uint32_t base = 0;
    for(regionMap::iterator itr = m_data.begin(); itr != m_data.end(); ++itr)
    {
        uint32_t offset = itr->first;
        uint32_t address = offset;
        std::vector<uint8_t>& data = itr->second;

        if((base & 0xFFFF0000) != (offset & 0xFFFF0000))
        {
            writeExtAddrLine(&file, offset);
            base = offset;
        }

        uint8_t write = 0;
        for(uint32_t i = 0; i != data.size(); i += write)
        {
            std::string line(":");
            write = (data.size() - i >= 0x10) ? 0x10 : data.size() - i;

            line += Utils::hexToString(write);       // recordn len
            line += Utils::hexToString(address >> 8); // address
            line += Utils::hexToString(address);
            line += "00";                            // record type

            uint8_t checksum = write + (uint8_t)(address >> 8) + (uint8_t)address;
            for(uint8_t x = 0; x < write; ++x)
            {
                line += Utils::hexToString(data[i+x]);
                checksum += data[i+x];
            }
            line += Utils::hexToString(0x100 - checksum);
            line += "\r\n";

            address += write;

            file.write(line.toAscii());
        }
    }

    static const std::string endFile = ":00000001FF\r\n";
    file.write(endFile.toAscii());
    file.close();
}

void HexFile::writeExtAddrLine(QFile *file, uint32_t addr)
{
    std::string line = ":02" "00" "00" "04";
    line += Utils::hexToString(addr >> 24);
    line += Utils::hexToString(addr >> 16);

    uint8_t checksum = 0x100 - (uint8_t)(0x02 + 0x04 + (uint8_t)(addr >> 24) + (uint8_t)(addr >> 16));
    line += Utils::hexToString(checksum);

    line += "\r\n";
    file->write(line.toAscii());
}

void HexFile::setData(const QByteArray &data)
{
    clear();

    uint32_t base = 0;
    uint32_t size = data.size();
    uint32_t itr = 0;
    uint32_t maxPerBase = 0xFFFF;

    std::vector<uint8_t> bytes;

    do
    {
        bytes.clear();

        for(;itr < size && itr < maxPerBase; ++itr)
            bytes.push_back(data[itr]);

        m_data[base] = bytes;
        base = itr;
        maxPerBase += 0xFFFF;
    }while((base & 0xFFFF0000) != (size & 0xFFFF0000));
}

QByteArray HexFile::getDataArray(uint32_t len)
{
    QByteArray res(len, 0xFF);
    uint32_t size = len;

    for(regionMap::iterator itr = m_data.begin(); itr != m_data.end(); ++itr)
    {
        uint32_t offset = itr->first;
        std::vector<uint8_t>& data = itr->second;

        for(uint32_t i = 0; i < data.size(); ++i)
        {
            if(len && offset + i > (uint32_t)res.size())
                return res;


            res[offset+i] = data[i];

            if(size < offset+i)
            {
                char *itr = res.data()+size;
                char *end = res.data()+offset+i-1;
                for(;itr < end; ++itr)
                    *itr = 0xFF;
            }
            size = offset+i;
        }
    }
    return res;
}*/

//template <typename OutputIterator>
//void make_pages(memory const & memory, std::string const & memid, chip_definition const & chip, OutputIterator out)
//program.hpp
void HexFile::makePages(std::vector<page> &pages, uint8_t memId, chip_definition &chip, std::set<uint32_t> *skipPages)
{
    chip_definition::memorydef const * memdef = chip.getMemDef(memId);
    if(!memdef)
    {
        char buff[64];
        sprintf(buff, "This chip does not have memory type %d", memId);
        throw std::string(buff);
    }

    if(getTopAddress() > memdef->size)
        throw std::string("Program is too large");

    if(memdef->pagesize == 0)
    {
        // The memory is unpaged.
        for(regionMap::iterator itr = m_data.begin(); itr != m_data.end(); ++itr)
        {
            page cur_page;
            cur_page.address = itr->first;
            cur_page.data = itr->second;
            pages.push_back(cur_page);
        }
    }
    else
    {
        page cur_page;
        cur_page.data.resize(memdef->pagesize);

        std::string patch_pos_str = (memId == MEM_FLASH) ? chip.getOption("avr232boot_patch") : "";
        uint32_t patch_pos = patch_pos_str.empty() ? 0 : atoi(patch_pos_str.c_str());

        uint32_t alt_entry_page = patch_pos / memdef->pagesize;
        bool add_alt_page = patch_pos != 0;

        Patcher patcher(patch_pos, memdef->size);

        for(uint32_t i = 0; i < memdef->size / memdef->pagesize; ++i)
        {
            cur_page.address = i * memdef->pagesize;
            if(!intersects(cur_page.address, memdef->pagesize))
                continue;

            std::fill(cur_page.data.begin(), cur_page.data.end(), 0xFF);
            getRange(cur_page.address, memdef->pagesize, cur_page.data);

            patcher.patchPage(cur_page);
            pages.push_back(cur_page);

            if(i == alt_entry_page)
                add_alt_page = false;
        }

        if(add_alt_page)
        {
            cur_page.address = alt_entry_page * memdef->pagesize;
            std::fill(cur_page.data.begin(), cur_page.data.end(), 0xFF);
            patcher.patchPage(cur_page);
            pages.push_back(cur_page);
        }
    }

    if(skipPages)
    {
        for(uint32_t i = 0; i < pages.size(); ++i)
        {
            bool skip = true;
            for(uint32_t x = 0; skip && x < pages[i].data.size(); ++x)
                if(pages[i].data[x] != 0xFF)
                    skip = false;

            if(skip)
                skipPages->insert(i);
        }
    }
}

bool HexFile::intersects(uint32_t address, uint32_t length)
{
    regionMap::iterator itr,prior;
    itr = prior = m_data.upper_bound(address);
    --prior;

    bool res = false;

    if(itr != m_data.begin() && prior->first + prior->second.size() > address)
        res = true;
    else if(itr != m_data.end() && itr->first < address + length)
        res = true;

    return res;
}

void HexFile::getRange(uint32_t address, uint32_t length, std::vector<uint8_t> & out)
{
    regionMap::iterator itr = m_data.upper_bound(address);
    if(itr != m_data.begin())
        --itr;

    for(; itr != m_data.end() && itr->first < address + length; ++itr)
    {
        uint32_t start = std::max(address, itr->first);
        uint32_t stop = std::min(address + length, (uint32_t)(itr->first + itr->second.size()));

        if(start >= stop)
            continue;

        std::copy(itr->second.begin() + (start - itr->first),
                  itr->second.begin() + (stop - itr->first),
                  out.begin() + (start - address));
    }
}
