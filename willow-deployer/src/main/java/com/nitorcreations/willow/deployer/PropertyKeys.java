package com.nitorcreations.willow.deployer;

public interface PropertyKeys {
	public static final String PROPERTY_KEY_LAUNCH_URLS                     = "deployer.launch.urls";
	public static final String PROPERTY_KEY_PREFIX_LAUNCH                   = "deployer.launch";
	public static final String PROPERTY_KEY_PREFIX_SHUTDOWN                 = "deployer.shutdown";
	public static final String PROPERTY_KEY_SHUTDOWN_DOWNLOAD               = "deployer.shutdown.download";
	public static final String PROPERTY_KEY_PREFIX_POST_STOP                = "deployer.post.stop";
	public static final String PROPERTY_KEY_PREFIX_POST_STOP_OLD            = "deployer.post.stop.old";
	public static final String PROPERTY_KEY_PREFIX_POST_DOWNLOAD            = "deployer.post.download";
	public static final String PROPERTY_KEY_PREFIX_PRE_DOWNLOAD             = "deployer.pre.download";
	
	
	public static final String PROPERTY_KEY_METHOD                          = ".method";
	public static final String PROPERTY_KEY_PREFIX_ARGS                     = ".arg";
	public static final String PROPERTY_KEY_BINARY                          = ".binary";
	public static final String PROPERTY_KEY_ARTIFACT                        = ".artifact";
	public static final String PROPERTY_KEY_EXTRA_ENV_KEYS                  = ".env.keys";
	public static final String PROPERTY_KEY_PREFIX_JAVA_ARGS                = ".java.arg";
	public static final String PROPERTY_KEY_RESOLVE_TRANSITIVE              = ".artifact.transitive";
	public static final String PROPERTY_KEY_MAIN_CLASS                      = ".java.mainclass";
	public static final String PROPERTY_KEY_CLASSPATH                       = ".java.classpath";
	public static final String PROPERTY_KEY_JAR                             = ".java.jar";
	public static final String PROPERTY_KEY_LAUNCH_WORKDIR                  = ".workdir";
	public static final String PROPERTY_KEY_AUTORESTART                     = ".autorestart";
	public static final String PROPERTY_KEY_TIMEOUT                         = ".timeout";
	public static final String PROPERTY_KEY_TERM_TIMEOUT                    = ".term.timeout";
	

	
	public static final String PROPERTY_KEY_PREFIX_DOWNLOAD_URL             = "deployer.download.url";
	public static final String PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH       = ".finalpath";
	public static final String PROPERTY_KEY_DOWNLOAD_DIRECTORY              = "deployer.download.directory";
	public static final String PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT        = "deployer.download.artifact";
	public static final String PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5             = ".md5";
	public static final String PROPERTY_KEY_SUFFIX_EXTRACT_INTERPOLATE_GLOB = ".extract.interpolate.glob";
	public static final String PROPERTY_KEY_SUFFIX_EXTRACT_ROOT             = ".extract.root";
	public static final String PROPERTY_KEY_SUFFIX_EXTRACT_GLOB             = ".extract.glob";
	public static final String PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB        = ".extract.skip.glob";
	public static final String PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB      = ".extract.filter.glob";
	public static final String PROPERTY_KEY_REMOTE_REPOSITORY               = "deployer.remote.repository";
	public static final String PROPERTY_KEY_STATISTICS_URI                  = "deployer.statistics.uri";
	public static final String PROPERTY_KEY_STATISTICS_FLUSHINTERVAL        = "deployer.statistics.flushinterval";
	public static final String PROPERTY_KEY_WORKDIR                         = "deployer.workdir";
	public static final String PROPERTY_KEY_PROPERTIES_FILENAME             = "deployer.properties.filename";
	public static final String PROPERTY_KEY_DEPLOYER_NAME                   = "deployer.name";
	public static final String PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX           = "deployer.launch.index";
	public static final String ENV_DEPLOYER_TERM_TIMEOUT                    = "DEPLOYER_TERM_TIMEOUT";
	public static final String ENV_DEPLOYER_LOCAL_REPOSITORY                = "DEPLOYER_LOCAL_REPOSITORY";
	public static final String ENV_DEPLOYER_NAME                            = "W_DEPLOYER_NAME";
}