# geoserver-plugin-contentlength
Content-Length Plugin as thirdparty GeoServer plugin

Content-Length Plugin
=====================

This plugin is created as thirdparty GeoServer plugin.
So, it is not included inside actual GeoServer source.
Then, the plugin can be built without building GeoServer 
itself same time.

GeoServer plugin
================

GeoServer plugins can be built by using Maven.
Browse plugin folders to find *pom.xml* and use Maven to compile plugin jar.
Use Maven in the *pom.xml* folder:
*mvn clean install*

Notice, jar-files are created to *target*-folder.

When jar-file is compiled, you can copy-paste it into *webapps\geoserver\WEB-INF\lib* -folder
of GeoServer. Then, the plugin is automatically taken into use after GeoServer is restarted.

*Notice*, if GeoServer is updated to a new version, you may also want to update GeoServer version
and plugin version information in *pom.xml* file and recompile the plugin for new GeoServer version.
