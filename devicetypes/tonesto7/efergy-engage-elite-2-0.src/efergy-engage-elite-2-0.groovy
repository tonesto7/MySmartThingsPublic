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
*	v2.2 (October 26th, 2015)
*	- Added Streamlined Tiles.  Added Tariff Rate Tile
*	v2.1 (Sept 18th, 2015)
*	- Remove debug logging from preferences.  It's now controlled from the smartapp
*
*	v2.0 (Sept 15th, 2015)
*	- Device is now installed and updated via the Efergy 2.0 (Connect) SmartApp
*  ---------------------------
*/
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

def devTypeVer() {"2.2"}
def versionDate() {"10-26-2015"}
	
metadata {
	definition (name: "Efergy Engage Elite 2.0", namespace: "tonesto7", author: "Anthony S.") {
		capability "Energy Meter"
        capability "Power Meter"
        capability "Polling"
        capability "Refresh"
        attribute "iconUrl", "string"
        command "poll"
        command "refresh"
        command "updateUsageData", ["string", "string", "string"]
		command "updateReadingData", ["string", "string"]
        command "updateTariffData", ["string"]
        command "updateHubData", ["string", "string", "string"]
        command "isDebugLogging", ["string"]
	}
    
	tiles (scale: 2) {
        multiAttributeTile(name:"power", type:"generic", width:6, height:4) {
    		tileAttribute("device.power", key: "PRIMARY_CONTROL") {
      			attributeState "default", label: '${currentValue} W', icon: "https://dl.dropboxusercontent.com/s/vfxkm0hp6jsl56m/power_icon_bk.png", 
                foregroundColor: "#000000",
                backgroundColors:[
					[value: 1, color: "#00cc00"], //Light Green
                	[value: 2000, color: "#79b821"], //Darker Green
                	[value: 3000, color: "#ffa81e"], //Orange
					[value: 4000, color: "#fb1b42"] //Bright Red
				]
    		}
        	tileAttribute("todayUsage", key: "SECONDARY_CONTROL") {
      				attributeState "default", label: '${currentValue}'
           	}
  		}
        
        valueTile("todayUsage", "device.todayUsage", width: 4, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}
        
        valueTile("monthUsage", "device.monthUsage", width: 4, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}
        
        valueTile("monthEst", "device.monthEst", width: 4, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}
        
        valueTile("tariffRate", "device.tariffRate", width: 2, height: 1, decoration: "flat") {
			state "default", label: 'Tariff Rate:\n${currentValue}/kWH'
		}
		
        valueTile("hubStatus", "device.hubStatus", width: 2, height: 1, decoration: "flat") {
			state "default", label: 'Hub Status:\n${currentValue}'
		}
        
        valueTile("hubVersion", "device.hubVersion", width: 2, height: 1, decoration: "flat") {
			state "default", label: 'Hub Version:\n${currentValue}'
		}
        
        valueTile("readingUpdated", "device.readingUpdated", width: 4, height: 2, decoration: "flat", wordWrap: true) {
			state "default", label:'${currentValue}'
	    }
        
        standardTile("refresh", "command.refresh", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        main (["power"])
        details(["power", "todayUsage", "monthUsage", "monthEst", "tariffRate", "hubStatus", "hubVersion", "readingUpdated", "refresh"])
	}
}

preferences {
	section() {
        
    }
}

// parse events into attributes
def parse(String description) {
	logWriter("Parsing '${description}'")
}
	
// refresh command
def refresh() {
	log.info "Refresh command received..."
    parent.refresh()
}
    
// Poll command
def poll() {
	log.info "Poll command received..."
    parent.refresh()
}

// Get extended energy metrics
def updateUsageData(String todayUsage, String monthUsage, String monthEst) {
    logWriter("--------------UPDATE USAGE DATA-------------")
	logWriter("todayUsage: " + todayUsage)
    logWriter("monthUsage: " + monthUsage)
    logWriter("monthEst: " + monthEst)
    logWriter("")
	sendEvent(name: "todayUsage", value: todayUsage, display: false, displayed: false)
    sendEvent(name: "monthUsage", value: monthUsage, display: false, displayed: false)
    sendEvent(name: "monthEst", value: monthEst, display: false, displayed: false)
}
 
def updateReadingData(String power, String readingUpdated) {
    logWriter("--------------UPDATE READING DATA-------------")
    logWriter("energy: " + power.toInteger() * 24 / 1000)
    logWriter("power: " + power)
    logWriter("readingUpdated: " + readingUpdated)
    logWriter("")    
    //Updates Device Readings to tiles
    sendEvent(name: "energy", unit: "kWh", value: power.toInteger() * 24 / 1000)
    sendEvent(name: "power", unit: "W", value: power)
    sendEvent(name: "readingUpdated", value: readingUpdated, display: false, displayed: false)
}

def updateTariffData(String tariffVal) {
    logWriter("--------------UPDATE TARIFF DATA-------------")
    logWriter("tariffVal: " + tariffVal)
    logWriter("")    
    //Updates Device Readings to tiles
    sendEvent(name: "tariffRate", value: tariffVal, display: false, displayed: false)
}

// Get Status 
def updateHubData(String hubVersion, String hubStatus, String hubName) {
    logWriter("--------------UPDATE HUB DATA-------------")
    logWriter("hubVersion: " + hubVersion)
    logWriter("hubStatus: " + hubStatus)
    logWriter("hubName: " + hubName)
    logWriter("")
	//Updates HubVersion and HubStatus Tiles 
	sendEvent(name: "hubVersion", value: hubVersion, display: false, displayed: false)
    sendEvent(name: "hubStatus", value: hubStatus, display: false, displayed: false)
    sendEvent(name: "hubName", value: hubName, display: false, displayed: false)
}    

def isDebugLogging(String showLogging) {
	state.showLogging = showLogging.toBoolean()
    logWriter("DebugLogging: ${state.showLogging}") 
}

def isShowHubTiles(String showHubTiles) {
	state.showHubTiles = showHubTiles.toBoolean()
    logWriter("ShowHubTiles: ${state.showHubTiles}") 
}
def isShowEstCostTiles(String showEstCostTiles) {
	state.showEstCostTiles = showEstCostTiles.toBoolean()
    logWriter("ShowEstCostTiles: ${state.showEstCostTiles}") 
}


//Log Writer that all logs are channel through *It will only output these if Debug Logging is enabled under preferences
private def logWriter(value) {
	if (state.showLogging) {
        log.debug "${value}"
    }	
}