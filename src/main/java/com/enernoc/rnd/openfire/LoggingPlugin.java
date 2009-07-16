package com.enernoc.rnd.openfire;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.log.Hierarchy;
import org.jivesoftware.util.log.LogTarget;
import org.jivesoftware.util.log.format.ExtendedPatternFormatter;
import org.jivesoftware.util.log.output.io.rotate.RevolvingFileStrategy;
import org.jivesoftware.util.log.output.io.rotate.RotateStrategyBySize;
import org.jivesoftware.util.log.output.io.rotate.RotatingFileTarget;

/**
 * Jesus fucking christ, do they really need to spread the log messages across 
 * 6 different log files?  What the fuck?  Did Jive invent logging before 
 * commons-logging, log4j or java.util.logging was invented?  Why the fuck 
 * did they roll their own and make it extra shitty????
 */
public class LoggingPlugin implements Plugin {

	public static final String CONFIG_JDK_LOGGING_PROPS = "logging.jdk.properties.url";
	String[] categories = new String[] { "Jive-DEBUG", "Jive-INFO", "Jive-WARN", "Jive-ERR" };
	
	static final String LOG_FILE_PROPERTY = "log.file.name";
	static final String DEFAULT_LOG_FILE = "info.log";
	static final String DEFAULT_LOG_CONFIG = "conf/logging.properties";
	
	@Override
	public void initializePlugin(PluginManager mgr, File pluginDir ) {
		String confFile = JiveGlobals.getProperty( CONFIG_JDK_LOGGING_PROPS, DEFAULT_LOG_CONFIG );
		InputStream urlStream = null; 
		try {
			System.out.println( "Reconfiguring logging from properties file: " + confFile );
			if ( confFile.contains( "://") ) urlStream = new URL(confFile).openStream();
			// assume it is a relative file path:
			else urlStream = new FileInputStream( new File( 
					JiveGlobals.getHomeDirectory(), confFile ) ); 
			 
			LogManager.getLogManager().readConfiguration( urlStream );
		}
		catch ( IOException ex ) {
			LogManager.getLogManager().getLogger("").log( 
					Level.SEVERE, "Error configuring logging", ex );
		}
		finally { if ( urlStream != null ) try { urlStream.close(); } catch ( IOException ex ) {} }
		
		
		Hierarchy logHierarchy = Hierarchy.getDefaultHierarchy();
		try {
			LogTarget target = new RotatingFileTarget(
					new ExtendedPatternFormatter( "%{priority} %{time:yyyy.MM.dd HH:mm:ss} [%{thread}] %{message}\\n%{throwable}" ), 
					new RotateStrategyBySize(20000 * 1024), // 20MB
					new RevolvingFileStrategy(getLogFile().getAbsolutePath(), 5) );
			for ( String category : categories )
				logHierarchy.getLoggerFor(category).setLogTargets(new LogTarget[] {target});
			Log.info( "Logging fixed" );
		}
		catch ( Exception ex ) {
			System.err.println( "WTF!" );
			ex.printStackTrace();
		}
	}
	
	protected File getLogFile() { // copied from OpenFire's original logging code
        String logDirectory = JiveGlobals.getXMLProperty("log.directory");
        if (logDirectory == null) {
            if (JiveGlobals.getHomeDirectory() != null) {
                File openfireHome = new File(JiveGlobals.getHomeDirectory());
                if (openfireHome.exists() && openfireHome.canWrite()) {
                    logDirectory = (new File(openfireHome, "logs")).toString();
                }
            }
        }

        if (!logDirectory.endsWith(File.separator))
            logDirectory = logDirectory + File.separator;

        // Make sure the logs directory exists. If not, make it:
        File logDir = new File(logDirectory);
        if (!logDir.exists()) logDir.mkdir();
        
        String fileName = JiveGlobals.getProperty( LOG_FILE_PROPERTY, DEFAULT_LOG_FILE );
        return new File( logDir, fileName );
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
	}
}