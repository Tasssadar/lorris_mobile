/**********************************************
**    This file is part of Lorris
**    http://tasssadar.github.com/Lorris/
**
**    See README and COPYING
***********************************************/

#ifndef CHIPDEFS_H
#define CHIPDEFS_H

#include <vector>
#include <map>
#include <stdint.h>
#include <string>

class chip_definition
{
public:

    struct memorydef
    {
        memorydef()
        {
            size = 0;
            pagesize = 0;
            memid = 0;
        }

        uint32_t size;
        uint16_t pagesize;
        uint8_t memid;
    };

    struct fuse
    {
        std::string name;
        std::vector<int> bits;
        std::vector<int> values;
    };

    template <typename Iter>
    static int get_fuse_value(Iter first, Iter last, fuse const & f);

    template <typename Iter>
    static void set_fuse_value(Iter first, Iter last, fuse const & f, int value);

    chip_definition();
    chip_definition(const std::string& sign);

    void copy(chip_definition& cd);

    const std::string& getName() { return m_name; }
    const std::string& getSign() { return m_signature; }
    void setName(const std::string& name) { m_name = name; }
    void setSign(const std::string& sign) { m_signature = sign; }

    std::map<std::string, memorydef> &getMems() { return m_memories; }
    std::vector<fuse> &getFuses() { return m_fuses; }
    std::map<std::string, std::string> &getOptions() { return m_options; }

    memorydef *getMemDef(const std::string& name)
    {
        std::map<std::string, memorydef>::iterator itr = m_memories.find(name);
        if(itr != m_memories.end())
            return &itr->second;
        return NULL;
    }

    memorydef *getMemDef(uint8_t memId);

    std::string getOption(const std::string& name)
    {
        std::string res;
        std::map<std::string, std::string>::iterator itr = m_options.find(name);
        if(itr != m_options.end())
            res = itr->second;
        return res;
    }

private:
    std::string m_name;
    std::string m_signature;

    std::map<std::string, memorydef> m_memories;
    std::map<std::string, std::string> m_options;

    std::vector<fuse> m_fuses;
};

template <typename Iter>
int chip_definition::get_fuse_value(Iter first, Iter last, fuse const & f)
{
    int fusevalue = 0;
    for (std::size_t j = f.bits.size(); j > 0; --j)
    {
        int bitno = f.bits[j-1];
        int byteno = bitno / 8;
        bitno %= 8;
        if (byteno >= last - first)
            continue;

        fusevalue = (fusevalue << 1) | !!(first[byteno] & (1<<bitno));
    }
    return fusevalue;
}

template <typename Iter>
void chip_definition::set_fuse_value(Iter first, Iter last, fuse const & f, int value)
{
    for (std::size_t j = 0; j != f.bits.size(); ++j)
    {
        int bitno = f.bits[j];
        int byteno = bitno / 8;
        bitno %= 8;

        if (byteno < last - first)
        {
            if (value & 1)
            {
                first[byteno] |= (1<<bitno);
            }
            else
            {
                first[byteno] &= ~(1<<bitno);
            }

            value >>= 1;
        }
    }
}

void load_chipdefs(char *data);
chip_definition *get_chipdef(const std::string& sign);

#endif // CHIPDEFS_H
