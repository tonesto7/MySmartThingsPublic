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
*	V2.4.0 (December 11th, 2015)
*	- Added Efergy's Monthly Budget value to the device tile
*   - Too many other changes to list
*	V2.3 (December 10th, 2015)
*	- Added TodayUsage Tiles to page and re-organized the tiles
*  ---------------------------
*/
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

def devTypeVer() {"2.4.0"}
def versionDate() {"12-11-2015"}
	
metadata {
	definition (name: "Efergy Engage Elite 2.0", namespace: "tonesto7", author: "Anthony S.") {
		capability "Energy Meter"
        capability "Power Meter"
        capability "Polling"
        capability "Refresh"
        attribute "iconUrl", "string"
        command "poll"
        command "refresh"
        command "updateStateData", ["string", "string", "string"]
        command "updateUsageData", ["string", "string", "string", "string", "string", "string"]
		command "updateReadingData", ["string", "string"]
        command "updateTariffData", ["string"]
        command "updateHubData", ["string", "string", "string"]
        command "isDebugLogging", ["string"]
	}
    
	tiles (scale: 2) {
        multiAttributeTile(name:"power", type:"thermostat", width:6, height:4, wordWrap: true) {
    		tileAttribute("device.power", key: "PRIMARY_CONTROL") {
      			attributeState "default", label: '${currentValue} W', icon: "https://dl.dropboxusercontent.com/s/vfxkm0hp6jsl56m/power_icon_bk.png", 
                foregroundColor: "#000000",
                backgroundColors:[
					[value: 1, color: "#00cc00"], //Light Green
                	[value: 2000, color: "#79b821"], //Darker Green
                	[value: 3000, color: "#ffa81e"], //Orange
					[value: 4000, color: "#FFF600"], //Yellow
                    [value: 5000, color: "#fb1b42"] //Bright Red
				]
    		}
        	tileAttribute("todayUsage", key: "SECONDARY_CONTROL") {
      				attributeState "default", label: 'Today\'s Usage: ${currentValue}'
           	}
  		}
        
        valueTile("todayUsage", "device.todayUsage", width: 3, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: 'Today\'s Usage:\n${currentValue}'
		}
        
        valueTile("monthUsage", "device.monthUsage", width: 3, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}
        
        valueTile("monthEst", "device.monthEst", width: 3, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label: '${currentValue}'
		}
        
        valueTile("budgetPercentage", "device.budgetPercentage", width: 3, height: 1, decoration: "flat", wordWrap: true) {
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
        details(["power", "todayUsage", "monthUsage", "monthEst", "budgetPercentage", "tariffRate", "hubStatus", "hubVersion", "readingUpdated", "refresh"])
	}
}

preferences {
}

// parse events into attributes
def parse(String description) {
	logWriter("Parsing '${description}'")
}
	
// refresh command
def refresh() {
	log.info "Refresh command received..."
    log.debug "budget: ${state.monthBudget}"
    parent.refresh()
}
    
// Poll command
def poll() {
	log.info "Poll command received..."
    parent.refresh()
}

def updateStateData(showLogging, monthName, currencySym) {
	if(showLogging) {
    	state.showLogging = showLogging.toBoolean()
    	logWriter("DebugLogging: ${state.showLogging}") 
    }
    if(monthName) {
    	state.monthName = monthName
        logWriter("Month: ${monthName}")
    }
    if(currencySym) {
    	state.currencySym = currencySym ?: ""
        logWriter("Currency Symbol: ${state.currencySym}")
    }
}

// Get extended energy metrics
def updateUsageData(todayUsage, todayCost, monthUsage, monthCost, monthEst, monthBudget) {
	if (monthBudget) {
    	state.monthBudget = monthBudget
    }
    def budgPercent = Math.round(Math.round(monthCost?.toFloat()) / Math.round(monthBudget?.toFloat()) * 100)
    
    logWriter("--------------UPDATE USAGE DATA-------------")
	logWriter("todayUsage: " + todayUsage + "kWh")
    logWriter("todayCost: " + state.currencySym+ todayCost)
    logWriter("monthUsage: " + monthUsage + " kWh")
    logWriter("monthCost: " + state.currencySym + monthCost)
    logWriter("monthEst: " + state.currencySym+ monthEst)
    logWriter("monthBudget: " + state.currencySym + monthBudget)
    logWriter("budget %: ${budgPercent}%")
    logWriter("")
	sendEvent(name: "todayUsage", 		value: "${state.currencySym}${monthCost} (${todayUsage} kWH)", display: false, displayed: false)
    sendEvent(name: "monthUsage", 		value: "${state.monthName}\'s Usage:\n${state.currencySym}${monthCost} (${monthUsage} kWh)", display: false, displayed: false)
    sendEvent(name: "monthEst", 		value: "${state.monthName}\'s Bill (Est.):\n${state.currencySym}${monthEst}", display: false, displayed: false)
    sendEvent(name: "budgetPercentage", value: "Using ${budgPercent}% of ${state.currencySym}${monthBudget} Monthly Budget", display: false, displayed: false)
}
 
def updateReadingData(String power, String readingUpdated) {
	def newTime = Date.parse("MMM d,yyyy - h:mm:ss a", readingUpdated).format("h:mm:ss a")
    def newDate = Date.parse("MMM d,yyyy - h:mm:ss a", readingUpdated).format("MMM d,yyyy")

	logWriter("--------------UPDATE READING DATA-------------")
    logWriter("energy: " + power.toInteger() / 1000)
    logWriter("power: " + power)
    logWriter("readingUpdated: " + readingUpdated)
    logWriter("")    
    //Updates Device Readings to tiles
    sendEvent(name: "energy", unit: "kWh", value: power.toInteger() / 1000, displayed: false)
    sendEvent(name: "power", unit: "W", value: power)
    sendEvent(name: "readingUpdated", value: "Last Updated:\n${newDate}\n${newTime}", display: false, displayed: false)
}

def updateTariffData(String tariffVal) {
    logWriter("--------------UPDATE TARIFF DATA-------------")
    logWriter("tariff rate: " + tariffVal)
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

//Log Writer that all logs are channel through *It will only output these if Debug Logging is enabled under preferences
private def logWriter(value) {
	if (state.showLogging) {
        log.debug "${value}"
    }	
}