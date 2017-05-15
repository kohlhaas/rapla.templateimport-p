package org.rapla.plugin.templateimport;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jetbrains.annotations.PropertyKey;
import org.rapla.components.i18n.AbstractBundle;
import org.rapla.components.i18n.BundleManager;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.inject.Extension;

@Extension(
    provides = I18nBundle.class,
    id = TemplateImportResources.BUNDLENAME )
@Singleton
public class TemplateImportResources extends AbstractBundle
{
    public static final String BUNDLENAME = "org.rapla.templateimport.TemplateImportResources";

    @Inject
    public TemplateImportResources( final BundleManager localeLoader )
    {
        super(BUNDLENAME, localeLoader);
    }

    @Override
    public String getString( @PropertyKey(
        resourceBundle = BUNDLENAME ) final String key )
    {
        return super.getString(key);
    }

    @Override
    public String getString( @PropertyKey(
        resourceBundle = BUNDLENAME ) final String key, final Locale locale )
    {
        return super.getString(key, locale);
    }

    @Override
    public String format( @PropertyKey(
        resourceBundle = BUNDLENAME ) final String key, final Object... obj )
    {
        return super.format(key, obj);
    }

}
