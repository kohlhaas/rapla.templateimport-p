package org.rapla.plugin.templateimport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParsedTemplateResult {

    List<String> header = new ArrayList<String>();
    List<Map<String, String>> templateList = new ArrayList<Map<String,String>>();
    
    public List<String> getHeader() {
        return header;
    }

    public List<Map<String, String>> getTemplateList() {
        return templateList;
    }

    public void setHeader(List<String> asList) {
        this.header = asList;
    }
    
    public void addTemplate(Map<String,String> row)
    {
        templateList.add( row );
    }

}
