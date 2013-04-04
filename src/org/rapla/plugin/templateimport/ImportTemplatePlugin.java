/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.templateimport;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;
import org.rapla.plugin.export2ical.Export2iCalChangeWatcher;
import org.rapla.plugin.export2ical.Export2iCalServlet;
import org.rapla.plugin.export2ical.ICalExport;
import org.rapla.plugin.export2ical.RaplaICalExport;
import org.rapla.plugin.export2ical.icalimport.ICalImport;
import org.rapla.plugin.export2ical.icalimport.RaplaICalImport;
import org.rapla.server.ServerService;

// Plugin will be available in the 1.7 release
public class ImportTemplatePlugin  implements PluginDescriptor
{
	public static final String RESOURCE_FILE = ImportTemplatePlugin.class.getPackage().getName() + ".ImportTemplateResources";
    public static final String PLUGIN_CLASS = ImportTemplatePlugin.class.getName();
    static boolean ENABLE_BY_DEFAULT = false;

    public String toString() {
        return "Import Templates";
    }

    /**
     * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.general.Container)
     */
    public void provideServices(Container container, Configuration config) {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE,I18nBundleImpl.createConfig( RESOURCE_FILE ) );
        if ( container.getContext().has( ServerService.ROLE) ){
            container.addContainerProvidedComponent(RaplaExtensionPoints.REMOTE_METHOD_FACTORY, RaplaTemplateImport.class.getName(), RaplaTemplateImport.class.getName(),config);
        } 
        else
        {
            container.addContainerProvidedComponent( RaplaExtensionPoints.CLIENT_EXTENSION, ImportTemplatePluginInitializer.class.getName(), PLUGIN_CLASS, config);
        }
        //container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, MyOption.class.getName(),PLUGIN_CLASS, config);

        
    }

    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
    }
}