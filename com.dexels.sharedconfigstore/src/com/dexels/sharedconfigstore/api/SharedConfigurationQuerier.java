package com.dexels.sharedconfigstore.api;

import java.util.List;
import java.util.Map;

public interface SharedConfigurationQuerier {
    /** 
     * Retrieve a list of known containers
     */
    public List<String> getContainers();
    
    /** 
     * 
     * @param container leave null for generic config
     */
    public Map<String, String> getConfiguration(String pid, String container);
}
