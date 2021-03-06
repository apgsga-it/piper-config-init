package com.apgsga.patch.service.bootstrap.config

import org.codehaus.groovy.runtime.ExceptionUtils

import groovy.sql.Sql.CreateCallableStatementCommand

/**
 * This command line Tool, is used for an intial setup of a cloned target Plattform
 * It configures the target relevant Configuration Properties of an Installation
 * It should'nt be run on a productive System.
 *
 */
public class PatchInitConfigCli {
	
	private boolean dryRun = true

	private PatchInitConfigCli() {
	}

	public static create() {
		return new PatchInitConfigCli()
	}

	def process(def args) {
		
		def cmdResults = new Expando()
		cmdResults.returnCode = 1
		def options = validateOpts(args)
		
		if (!options) {
			cmdResults.returnCode = 0
			return cmdResults
		}
	
		try {
			cmdResults.result = initConfiguration(options)
			cmdResults.returnCode = 0
			return cmdResults
			
		} catch (AssertionError e) {
			System.err.println "Client Error ccurred ${e.message} "
			return cmdResults
		} catch (Exception e) {
			System.err.println "Unhandled Exception occurred "
			System.err.println e.toString()
			return cmdResults
		}
	}
	
	private def initConfiguration(def options) {
		// TODO JHE: Get de default using another way ... config file? possible to have a default from validateOpts?
		def initConfigFile = "/etc/opt/apg-patch-target-configinit/init.properties"
		if(options.i) {
			initConfigFile = options.is[0]
		}
		println "Provided configuration file was : ${initConfigFile}"
		def initConfig = parseConfig(initConfigFile)
		verifyHost(initConfig)
		def initClient = new PatchInitConfigClient(initConfig,dryRun)
		initClient.initAll()
	}
	
	private def verifyHost(def config) {
		// Aim of this method is to prevent the initialization to occur on an undesired host.
		// Therefore, the developer must explicitly set the running.host.name property to match the hostname.
		// This is a very poorman check ... but should just help preventing a bad mistake
		def hostname = InetAddress.getLocalHost().getHostName()
		if(!config.running.host.name.equals(hostname)) {
			throw new RuntimeException("running.host.name property is not set correctly, please verify that you're running the tool on the right environment")
		}
	}
	
	private def parseConfig(def initConfigFile) {
		ConfigSlurper cs = new ConfigSlurper()
		def props = new Properties()
		new File(initConfigFile).withInputStream{ stream -> 
			props.load(stream)	
		}
		cs.parse(props)
	}
 

	private def validateOpts(def args) {
		def cli = new CliBuilder(usage: 'patchinitcli.sh -[h|i|]')
		cli.formatter.setDescPadding(0)
		cli.formatter.setLeftPadding(0)
		cli.formatter.setWidth(100)
		cli.with {
			h longOpt: 'help', 'Show usage information', required: false
			i longOpt: 'init', args:1, argName: 'fileName', 'Location of config Property File. A property file is provided into /etc/opt/apg-patch-target-configinit', required: false
			dr longOpt: 'dryrun', args:1, argName: 'value', 'true/false, true by default', required: false
		}

		def options = cli.parse(args)
		def error = true

		if (!options) {
			return null
		}
		
		if(options.size)

		if(options.h) {
			cli.usage()
			return null
		}

		if(options.i) {
			error = false
		}
		
		if(options.dr) {
			if(options.drs[0].equals("false")) {
				this.dryRun = false
			}
		}
	
		if(error) {
			cli.usage()
			return null
		}

		options
	}
}