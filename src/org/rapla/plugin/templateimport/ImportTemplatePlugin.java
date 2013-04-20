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
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.TypedComponentRole;
import org.rapla.plugin.RaplaClientExtensionPoints;
import org.rapla.server.ServerService;
import org.rapla.server.ServerServiceContainer;

// Plugin will be available in the 1.7 release
public class ImportTemplatePlugin  implements PluginDescriptor
{
	public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>(ImportTemplatePlugin.class.getPackage().getName() + ".ImportTemplateResources");
    public static final String PLUGIN_CLASS = ImportTemplatePlugin.class.getName();
    static boolean ENABLE_BY_DEFAULT = false;

    public String toString() {
        return "Import Templates";
    }

    /**
     * @throws RaplaContextException 
     * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.general.Container)
     */
    public void provideServices(Container container, Configuration config) throws RaplaContextException {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( RESOURCE_FILE, I18nBundleImpl.class,I18nBundleImpl.createConfig( RESOURCE_FILE.getId() ) );
        RaplaContext context = container.getContext();
        if ( context.has( ServerService.class) )
        {
            context.lookup( ServerServiceContainer.class).addRemoteMethodFactory(TemplateImport.class,RaplaTemplateImport.class);
        } 
        else
        {
            container.addContainerProvidedComponent( RaplaClientExtensionPoints.CLIENT_EXTENSION, ImportTemplatePluginInitializer.class);
        }
        //container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, MyOption.class.getName(),PLUGIN_CLASS, config);

        
    }

}