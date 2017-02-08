/**********************************************
**    This file is part of Lorris
**    http://tasssadar.github.com/Lorris/
**
**    See README and COPYING
***********************************************/

#ifndef HEXFILE_H
#define HEXFILE_H

#include <map>
#include <vector>
#include <set>
#include <string>
#include <stdint.h>

class chip_definition;

enum MemoryTypes
{
    MEM_FLASH   = 1,
    MEM_EEPROM  = 2,
    MEM_FUSES   = 3,
    MEM_SDRAM   = 4,

    MEM_COUNT   = 5
};

struct page
{
    uint32_t address;
    std::vector<uint8_t> data;
};

class HexFile
{
public:
    typedef std::map<uint32_t, std::vector<uint8_t> > regionMap;

    class Patcher
    {
    public:
        Patcher(uint32_t patch_pos, uint32_t boot_reset)
        {
            m_patch_pos = patch_pos;
            m_boot_reset = boot_reset;
            m_entrypt_jmp = 0;
        }

        void patchPage(page& p);

    private:
        uint16_t m_entrypt_jmp;
        uint32_t m_patch_pos;
        uint32_t m_boot_reset;
    };

    HexFile();

    void clear()
    {
        m_data.clear();
    }

    void LoadFromFile(const std::string& path);
    //void SaveToFile(const std::string& path);

    void addRegion(uint32_t pos, std::vector<uint8_t>::iterator first, std::vector<uint8_t>::iterator last, int lineno);

    regionMap& getData() { return m_data; }
    uint32_t getProgSize() const;
    //void setData(const & data);
    //QByteArray getDataArray(quint32 len);

    uint32_t getTopAddress()
    {
        if(m_data.empty())
            return 0;
        regionMap::iterator last = m_data.end();
        --last;
        return last->first + last->second.size();
    }

    std::vector<uint8_t>& operator[](uint32_t i)
    {
        return m_data[i];
    }

    void makePages(std::vector<page>& pages, uint8_t memId, chip_definition& chip, std::set<uint32_t> *skipPages);
    bool intersects(uint32_t address, uint32_t length);
    void getRange(uint32_t address, uint32_t length, std::vector<uint8_t> & out);

private:
    //void writeExtAddrLine(QFile *file, quint32 addr);

    regionMap m_data;
};

#endif // HEXFILE_H
