/**
*  Efergy Engage Energy Monitor
*	
*  Copyright 2015 Anthony S.
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  ---------------------------
*	v2.0 (Sept 15th, 2015)
*	- Device is now installed and updated via the Efergy 2.0 (Connect) SmartApp
*
*  v1.1 (Sept 11th, 2015)
*	- Trying to update the tile view to reflect the new multiAttributeTile features
*  	- Add Month Name to tiles.
*  
*  v1.1 (August 23rd, 2015)
*  	- Reworked and optimized most of http code to handle the different clamp types
*   - Added Refresh button to manually update the info as you wish
*  	- Added preference setting to enable to debug logging if you are having issues
*	- Uploading to GitHub
*  ---------------------------
*/
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

def devTypeVer() {"2.0"}
def versionDate() {"9-15-2015"}
	
metadata {
	definition (name: "Efergy Engage Elite 2.0", namespace: "tonesto7", author: "Anthony S.") {
		capability "Energy Meter"
        capability "Power Meter"
        capability "Polling"
        capability "Refresh"
        command "poll"
        command "refresh"
        command "updateUsageData", ["string", "string", "string"]
		command "updateReadingData", ["string", "string"]
        command "updateHubData", ["string", "string"]
	}
    
	tiles (scale: 2) {
        multiAttributeTile(name:"power", type:"generic", width:6, height:4) {
    		tileAttribute("device.power", key: "PRIMARY_CONTROL") {
      			attributeState "default", label: '${currentValue} W', icon: "https://dl.dropboxusercontent.com/s/bdj3636ohmxkgr5/power_icon.png", 
                foregroundColor: "#000000",
                backgroundColors:[
					[value: 1, color: "#00cc00"], //Light Green
                	[value: 2000, color: "#79b821"], //Darker Green
                	[value: 3000, color: "#ffa81e"], //Orange
					[value: 4000, color: "#fb1b42"] //Bright Red
				]
    		}
			tileAttribute("device.todayUsage", key: "SECONDARY_CONTROL") {
      			attributeState "default", label: '${currentValue}'
            }
  		}
        
        valueTile("monthUsage", "device.monthUsage", width: 4, height: 2, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}    
        
        valueTile("monthEst", "device.monthEst", width: 4, height: 2, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}   
        
        valueTile("readingUpdated", "device.readingUpdated", width: 6, height: 2, decoration: "flat", wordWrap: true) {
			state "default", label:'${currentValue}'
	    }
        
        valueTile("hubStatus", "device.hubStatus", width: 2, height: 1, decoration: "flat") {
			state "default", label: 'Hub Status:\n${currentValue}'
		}
        
        valueTile("hubVersion", "device.hubVersion", width: 2, height: 1, decoration: "flat") {
			state "default", label: 'Hub Version:\n${currentValue}'
		}
        
        standardTile("refresh", "command.refresh", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
		state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        main (["power"])
        details(["power", "monthUsage", "monthEst", "readingUpdated", "refresh", "hubStatus", "hubVersion"])
	}
}

preferences {
	section() {
        paragraph "Enable this if you are having issues with tiles not updating...\n** This will create alot of Log Entries so its recommended that you disable when it's not needed **"
        input "showLogging", "bool", title: "Enable Debug Logging", required: false, displayDuringSetup: false, defaultValue: false
        if(showLogging == true){ 
        	state.showLogging = true
            log.debug "Debug Logging Enabled!!!"    
        }
        if(showLogging == false){ 
        	state.showLogging = false 
            log.debug "Debug Logging Disabled!!!"                
        }
    }
}

// parse events into attributes
def parse(String description) {
	logWriter("Parsing '${description}'")
}
	
// refresh command
def refresh() {
	log.debug "Refresh command received..."
    parent.refresh()
}
    
// Poll command
def poll() {
	log.debug "Poll command received..."
    refresh()
}

// Get extended energy metrics
def updateUsageData(String todayUsage, String monthUsage, String monthEst) {
	logWriter("todayUsage: " + todayUsage)
    logWriter("monthUsage: " + monthUsage)
    logWriter("monthEst: " + monthEst)
	sendEvent(name: "todayUsage", value: todayUsage)
    sendEvent(name: "monthUsage", value: monthUsage)
    sendEvent(name: "monthEst", value: monthEst)
}
 
def updateReadingData(String power, String readingUpdated) {
	logWriter("energy: " + power.toInteger() / 1000)
    logWriter("power: " + power)
    logWriter("readingUpdated: " + readingUpdated)
	//Updates Device Readings to tiles
    sendEvent(name: "energy", unit: "kWh", value: power.toInteger() / 1000)
    sendEvent(name: "power", unit: "W", value: power)
    sendEvent(name: "readingUpdated", value: readingUpdated)
}

// Get Status 
def updateHubData(String hubVersion, String hubStatus) {
	logWriter("hubVersion: " + hubVersion)
    logWriter("hubStatus: " + hubStatus)
	//Updates HubVersion and HubStatus Tiles 
	sendEvent(name: "hubVersion", value: hubVersion)
    sendEvent(name: "hubStatus", value: hubStatus)
}    

//Log Writer that all logs are channel through *It will only output these if Debug Logging is enabled under preferences
private def logWriter(value) {
	if (state.showLogging) {
        log.debug "${value}"
    }	
}