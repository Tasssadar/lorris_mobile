/**********************************************
**    This file is part of Lorris
**    http://tasssadar.github.com/Lorris/
**
**    See README and COPYING
***********************************************/

#include <vector>
#include <map>
#include <algorithm>
#include <stdlib.h>
#include "chipdefs.h"

static std::map<std::string, chip_definition> chipdefs = std::map<std::string, chip_definition>();

static void splitString(std::string str, char delimiter, std::vector<std::string>& res)
{
    std::size_t idx = 0, idx_next = 0, len;
 
    do 
    {
        idx_next = str.find(delimiter, idx+1);
        if(idx != 0)
            ++idx;
 
        len = std::min(idx_next, str.size()) - idx;
 
        res.push_back(str.substr(idx, len));
        idx = idx_next;
    } while(idx != std::string::npos);
 
    if(res.empty())
        res.push_back(str);
}

void load_chipdefs(char *data)
{
    chipdefs.clear();

    for(char *p = strtok(data, "\r\n"); p != NULL; p = strtok(NULL, "\r\n"))
    {
        if(p[0] == '#')
            continue;

        std::vector<std::string> tokens;
        splitString(p, ' ', tokens);
        if(tokens.size() < 2)
            continue;
        
        chip_definition def;
        def.setName(tokens[0]);
        def.setSign(tokens[1]);
        
        // parse memories
        if(tokens.size() > 2)
        {
            std::vector<std::string> memories;
            splitString(tokens[2], ',', memories);
            for(uint8_t i = 0; i < memories.size(); ++i)
            {
                std::vector<std::string> memory_tokens;
                splitString(memories[i], '=', memory_tokens);
                if(memory_tokens.size() != 2)
                    continue;
                
                std::vector<std::string> mem_size_tokens;
                splitString(memory_tokens[1], ':', mem_size_tokens);
                
                chip_definition::memorydef memdef;
                memdef.memid = i + 1;
                memdef.size = atoi(mem_size_tokens[0].c_str());
                if(mem_size_tokens.size() > 1)
                    memdef.pagesize = atoi(mem_size_tokens[1].c_str());
                else
                    memdef.pagesize = 0;
                def.getMems()[memory_tokens[0]] = memdef;
            }
        }
        
        // parse fuses
        for(size_t i = 3; i < tokens.size(); ++i)
        {
            std::string& tok = tokens[i];
            if(tok[0] == '!')
            {
                size_t sep_pos = tok.find('=');
                if(sep_pos == std::string::npos)
                    throw "Invalid syntax in the chip definition file!";

                std::map<std::string, std::string> &opt = def.getOptions();
                opt[tok.substr(1, sep_pos - 1)] = tok.substr(sep_pos +1); 
            }
            else
            {
                std::vector<std::string> token_parts;
                splitString(tok, ':', token_parts);
                if(token_parts.size() != 2 && token_parts.size() != 3)
                    continue;
                
                std::vector<std::string> bit_numbers;
                splitString(token_parts[1], ',', bit_numbers);
                
                chip_definition::fuse f;
                f.name = token_parts[0];
                for(size_t j = 0; j < bit_numbers.size(); ++j)
                    f.bits.push_back(atoi(bit_numbers[j].c_str()));
                
                if(token_parts.size() == 3)
                {
                    bit_numbers.clear();
                    splitString(token_parts[2], ',', bit_numbers);
                    for(size_t j = 0; j < bit_numbers.size(); ++j)
                        f.values.push_back(atoi(bit_numbers[j].c_str()));
                }
                def.getFuses().push_back(f);
            }
        }

        // If chip with this signature is already loaded, rewrite it
        chipdefs[def.getSign()] = def;
    }
}

chip_definition *get_chipdef(const std::string& sign)
{
    chip_definition *cd = new chip_definition(sign);

    std::map<std::string, chip_definition>::iterator itr = chipdefs.find(sign);
    if(itr == chipdefs.end())
        return cd;

    cd->copy(itr->second);

    if(cd->getMems().find("fuses") == cd->getMems().end() && cd->getSign().find("avr:") == 0)
    {
        chip_definition::memorydef mem;
        mem.memid = 3;
        mem.size = 4;
        mem.pagesize = 0;
        cd->getMems()["fuses"] = mem;
    }
    return cd;
}

chip_definition::chip_definition()
{
}

chip_definition::chip_definition(const std::string &sign)
{
    m_signature = sign;
}

chip_definition::memorydef *chip_definition::getMemDef(uint8_t memId)
{
    static const std::string memNames[] = { "", "flash", "eeprom", "fuses", "sdram" };
    return getMemDef(memNames[memId]);
}

void chip_definition::copy(chip_definition &cd)
{
    m_name = cd.getName();
    m_memories = cd.getMems();

    for(uint32_t x = 0; x < cd.getFuses().size(); ++x)
    {
        uint32_t k;
        for(k = 0; k < m_fuses.size(); ++k)
        {
            if(m_fuses[k].name == cd.getFuses()[x].name)
                break;
        }

        if(k == m_fuses.size())
            m_fuses.push_back(cd.getFuses()[x]);
    }
}
