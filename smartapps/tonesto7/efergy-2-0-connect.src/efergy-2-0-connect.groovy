/**
 *  Efergy 2.0 (Connect)
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
 *  v2.3 (Oct 1st, 2015)
 *	- Added the new single instance only platform feature. to prevent multiple installs of this service manager
 *  v2.2 (Sept 28th, 2015)
 *	- Reworked Scheduling Mechanism. The application now checks for updates to the timestamp every 5 minutes
 *	- If there hasn't been an update it reschedules all of the Jobs
 *	- Notifications now work correctly (I think)
 *  - Please don't judge the devices updates by the activity log in the mobile app.  It is not very accurate.
 *
 *	v2.1 (Sept 18th, 2015)
 *	- Enabling Debug logging in SmartApp also enables it in the Device type.
 *
 *	v2.0 (Sept 15th, 2015)
 *	- Device is now installed and updated via the Efergy 2.0 (Connect) SmartApp
 *
 *   ---------------------------
 */
 
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

definition(
	name: "${textAppName()}",
    namespace: "${textNamespace()}",
    author: "${textAuthor()}",
    description: "${textDesc()}",
	category: "My Apps",
	iconUrl:   "https://dl.dropboxusercontent.com/s/daakzncm7zdzc4w/efergy_128.png",
	iconX2Url: "https://dl.dropboxusercontent.com/s/ysqycalevj2rvtp/efergy_256.png",
	iconX3Url: "https://dl.dropboxusercontent.com/s/56740lxra2qkqix/efergy_512.png",
    singleInstance: true)

//Change This to rename the Defaul App Name
def appName() { "Efergy 2.0 (Connect)" }
//Pretty Self Explanatory
def appAuthor() { "Anthony S." }
//So is this...
def appNamespace() { "tonesto7" }
//This one too...
def appVersion() { "2.3.0" }
//Definitely this one too!
def versionDate() { "10-01-2015" }
//Application Description
def appDesc() { "This app will connect to the Efergy Servers and create the device automatically for you.  It will also update the device info every 30ish seconds" }

preferences {
	page(name: "startPage")
    page(name: "loginPage", title: "Efergy Login")
    page(name: "prefPage", title: "Preferences")
	page(name: "hubInfoPage", title: "Hub Information")
}

def startPage() {
	if (state.efergyAuthToken != null) { return prefPage() }
    else { return loginPage() }
}

def loginPage() {
    def showUninstall = username != null && password != null 
   	return dynamicPage(name: "loginPage", title: "Efergy Login", nextPage:"prefPage", uninstall: showUninstall, install: false) {
    	
		section("Efergy Login Page") {
        	paragraph "Please enter your https://engage.efergy.com login credentials to generate you Authentication Token and install the device automatically for you."
			input("username", "email", title: "Username", description: "Efergy Username (email address)")
			input("password", "password", title: "Password", description: "Efergy Password")
		}
	}
}

/* Preferences */
def prefPage() {
	if(state.showLogging == null) { state.showLogging = false }
    if(state.darkIcon == null) { state.darkIcon = false 
        logWriter("Dark Icon was null but was changed to False") }
    if(state.efergyAuthToken == null) { getAuthToken() }
    
	dynamicPage(name: "prefPage", title: "Preferences", uninstall: true, install: true) {
        section("App Details:") {
        	paragraph "Name: ${textAppName()}\nCreated by: Anthony S.\n${textVersion()}\n${textModified()}\nGithub: @tonesto7", image: "https://dl.dropboxusercontent.com/s/daakzncm7zdzc4w/efergy_128.png"
    	}
        section("App Description:"){
        	paragraph "${textDesc()}"
        }	

		section() { 
        	href "hubInfoPage", title:"View Hub Info", description: "Tap to view more..." 
        }
        
		section("More Options", hidden: false, hideable: true){
            input("recipients", "contact", title: "Send notifications to", required: false, submitOnChange: true) {
            	input "phone", "phone", title: "Warn with text message (optional)",
                	description: "Phone Number", required: false, submitOnChange: true
        	}
            // Set Notification Recipients  
            if (location.contactBookEnabled && recipients) {
            	input "notifyAfterMin", "number", title: "Send Notification after (X) minutes of no updates", required: false, defaultValue: "60", submitOnChange: true
                input "notifyDelayMin", "number", title: "Only Send Notification every (x) minutes...", required: false, defaultValue: "50", submitOnChange: true
                state.notifyAfterMin = notifyAfterMin
                state.notifyDelayMin = notifyDelayMin         
            }
            
            paragraph "This will help if you are having issues with data not updating...\n** This will generate a TON of Log Entries so only enable if needed **"
        	input "showLogging", "bool", title: "Enable Debug Logging", required: false, displayDuringSetup: false, defaultValue: false, submitOnChange: true
        	if(showLogging && !state.showLogging) { 
            	state.showLogging = true
            	log.debug "Debug Logging Enabled!!!"
                refresh()
            }
        	if(!showLogging && state.showLogging){ 
            	state.showLogging = false 
            	log.debug "Debug Logging Disabled!!!"
                refresh()
            }
		}
   	}
}

def hubInfoPage () {
	if (state.hubName == null) { refresh() }
	dynamicPage(name: "hubInfoPage", install: false) {
 		section ("Hub Information") {
    		paragraph "Hub Name: " + state.hubName
        	paragraph "Hub ID: " + state.hubId
        	paragraph "Hub Mac Address: " + state.hubMacAddr
        	paragraph "Hub Status: " + state.hubStatus
        	paragraph "Hub Data TimeStamp: " + state.hubTsHuman
        	paragraph "Hub Type: " + state.hubType
        	paragraph "Hub Firmware: " + state.hubVersion
        }
    }
 }

/* Initialization */
def installed() { 
	initialize() 
}

def updated() { 
	unsubscribe()
	initialize() 
}

def uninstalled() {
	unsubscribe()
	unschedule()
	removeChildDevices(getChildDevices())
}
    
def initialize() {    
	refresh()
	addDevice()	
   	addSchedule()
    evtSubscribe()
}

//subscribes to the various location events and uses them to refresh the data if the scheduler gets stuck
private evtSubscribe() {
	subscribe(location, "sunrise", refresh)
	subscribe(location, "sunset", refresh)
	subscribe(location, "mode", refresh)
	subscribe(location, "sunriseTime", refresh)
	subscribe(location, "sunsetTime", refresh)
}

//Creates the child device if it not already there
private addDevice() {
	def dni = "Efergy Engage|" + state.hubMacAddr
    state.dni = dni
  	def d = getChildDevice(dni)
  	if(!d) {
    	d = addChildDevice("tonesto7", "Efergy Engage Elite 2.0", dni, null, [name:"Efergy Engage Elite", label: "Efergy Engage Elite", completedSetup: true])
    	d.take()
    	logWriter("created ${d.displayName} with id $dni")
  	} 
    else {
    logWriter("Device already created")
  	}
}

private removeChildDevices(delete) {
	try {
    	delete.each {
        	deleteChildDevice(it.deviceNetworkId)
    		}
   		}
    catch (e) { logWriter("There was an error (${e}) when trying to delete the child device") }
}

//Sends updated reading data to the Child Device
def updateDeviceData() {
	logWriter(" ")
    logWriter("--------------Sending Data to Device--------------")
	getAllChildDevices().each { 
    	it.isDebugLogging(state.showLogging.toString())
    	it.updateReadingData(state.powerReading.toString(), state.readingUpdated)
		it.updateUsageData(state.todayUsage, state.monthUsage, state.monthEst)
		it.updateHubData(state.hubVersion, state.hubStatus)
	}
}

// refresh command
def refresh() {
	checkSchedule()
    logWriter("")	
	log.debug "Refreshing Efergy Energy data from engage.efergy.com"
    
    getDayMonth()
    getReadingData()
 	getUsageData()
    getHubData()
    
    //If any people have been added for notification then it will check to see if it should notify
    if (recipients) { checkForNotify() }
   
    updateDeviceData()
    logWriter("")
}

//Create Refresh schedule to refresh device data (Triggers roughly every 30 seconds)
private addSchedule() {
    schedule("0 0/5 * 1/1 * ? *", "checkSchedule")
    schedule("1/1 * * * * ?", "refresh")
}

private checkSchedule() {
	def timeSince
   	if (state.hubTsHuman !=null) {
    	timeSince = GetTimeDiffSeconds(state.hubTsHuman)
    }
    if (timeSince == null || timeSince > 360) {
    	log.warn "It has been more that 5 minutes since last refresh"
        log.debug "Scheduling Issue found Re-Initializing Schedule... Data should resume refreshing in 30 seconds" 
        addSchedule()
    }
}

// Get Efergy Authentication Token
def getAuthToken() {
	def closure = { 
    	resp -> 
        log.debug("Auth Response: " + resp.data)  
        if (resp.data.status == "ok") { 
        	state.efergyAuthToken = resp.data.token       
        }
    }
	def params = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile/get_token",
        query: ["username": settings.username, "password": settings.password, "device": "website"],
        contentType: 'application/json'
    	]
	httpGet(params, closure)
    refresh()
}

//Converts Today's DateTime into Day of Week and Month Name ("September")
def getDayMonth() {
	def sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy")
    def now = new Date()
    def month
    def day
    month = new SimpleDateFormat("MMMM").format(now)
    day = new SimpleDateFormat("EEEE").format(now)
   
    if (month != null && day != null) {
    	state.monthName = month
        state.dayOfWeek = day
    } 
}

//Checks for Time passed since last update and sends notification if enabled
def checkForNotify() {
    if(state.notifyDelayMin == null) { state.notifyDelayMin = 50 }
    if(state.notifyAfterMin == null) { state.notifyAfterMin = 60 }
    logWriter("Delay X Min: " + state.notifyDelayMin)
    logWriter("After X Min: " + state.notifyAfterMin)
    def delayVal = state.notifyDelayMin * 60
    def notifyVal = state.notifyAfterMin * 60
    
    def lastNotifySeconds = GetTimeDiffSeconds(state.lastNotified)
    def timeSince = GetTimeDiffSeconds(state.hubTsHuman)
    
    if (lastNotifySeconds != null && state.lastNotified != null) {
    	state.lastNotifySeconds = lastNotifySeconds
        logWriter("Last Notified: ${state.lastNotified} - (${state.lastNotifySeconds} seconds ago)")
    }

	else if (lastNotifySeconds == null || state.lastNotified == null) {
    	state.lastNotifySeconds = 0
        state.lastNotified = "Mon Jan 01 00:00:00 2000"
        logWriter("Error getting last Notified: ${state.lastNotified} - (${state.lastNotifySeconds} seconds ago)")
        return
    }
    
    if (timeSince > delayVal) {
        if (state.lastNotifySeconds < notifyVal){
        	logWriter("Notification was sent ${state.lastNotifySeconds} seconds ago.  Waiting till after ${notifyVal} seconds before sending Notification again!")
            return
        }
        else {
        	state.lastNotifySeconds = 0
        	NotifyOnNoUpdate(timeSince)
        }
    }
}

//Sends the actual Push Notification
def NotifyOnNoUpdate(Integer timeSince) {
	def now = new Date()
	def notifiedDt = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy")
    state.lastNotified = notifiedDt.format(now)
	    
    def message = "Something is wrong!!! Efergy Device has not updated in the last ${timeSince} seconds..."
	
	if (location.contactBookEnabled && recipients) {
        //log.debug "contact book enabled!"
        sendNotificationToContacts(message, recipients)
    } else {
        logWriter("contact book not enabled")
        if (phone) {
            sendSms(phone, message)
        }
    }
}

//Returns time difference is seconds 
def GetTimeDiffSeconds(String startDate) {
	def now = new Date()
    def startDt = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy").parse(startDate)
    def result
    def diff = now.getTime() - startDt.getTime()  
    def diffSeconds = (int) (long) diff / 1000
    //def diffMinutes = (int) (long) diff / 60000
    return diffSeconds
}

//Matches hubType to a full name
def getHubName(String hubType) {
	def hubName = ""
    switch (hubType) {
   		case 'EEEHub':
       		hubName = "Efergy Engage Elite Hub"
       	break
        default:
       		hubName "unknown"
	}
    state.hubName = hubName
}

// Get extended energy metrics
def getUsageData() {
	def estUseClosure = { 
        estUseResp -> 
            //Sends extended metrics to tiles
            state.todayUsage = "Today\'s Usage: \$${estUseResp.data.day_tariff.estimate} (${estUseResp.data.day_kwh.estimate} kWh)"
            state.monthUsage = "${state.monthName} Usage \$${estUseResp.data.month_tariff.previousSum} (${estUseResp.data.month_kwh.previousSum} kWh)"
            state.monthEst = "${state.monthName}\'s Cost (Est.) \$${estUseResp.data.month_tariff.estimate}"
            
            //Show Debug logging if enabled in preferences
            logWriter(" ")
            logWriter("-------------------ESTIMATED USAGE DATA-------------------")
            //logWriter("Http Usage Response: $estUseResp.data")
            logWriter("TodayUsage: Today\'s Usage: \$${estUseResp.data.day_tariff.estimate} (${estUseResp.data.day_kwh.estimate} kWh)")
            logWriter("MonthUsage: ${state.monthName} Usage \$${estUseResp.data.month_tariff.previousSum} (${estUseResp.data.month_kwh.previousSum} kWh)")
            logWriter("MonthEst: ${state.monthName}\'s Cost (Est.) \$${estUseResp.data.month_tariff.estimate}")
		}
        
	def params = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile_proxy/getEstCombined",
        query: ["token": state.efergyAuthToken],
        contentType: 'application/json'
    	]
	httpGet(params, estUseClosure)
}
 
/* Get the sensor reading
****  Json Returned: {"cid":"PWER","data":[{"1440023704000":0}],"sid":"123456","units":"kWm","age":142156},{"cid":"PWER_GAC","data":[{"1440165858000":1343}],"sid":"123456","units":null,"age":2}
*/
def getReadingData() {
    def today = new Date()
    def tf = new SimpleDateFormat("M/d/yyyy - h:mm:ss a")
    	tf.setTimeZone(TimeZone.getTimeZone("America/New_York"))
    def cidVal = "" 
	def cidData = [{}]
    def cidUnit = ""
    def timeVal
	def cidReading
    def cidReadingAge
    def readingUpdated
    def summaryClosure = { 
        summaryResp -> 
        	def respData = summaryResp.data.text
            
            //Converts http response data to list
			def cidList = new JsonSlurper().parseText(respData)
			
            //Search through the list for age to determine Cid Type
			for (rec in cidList) { 
    			if ((((rec.age >= 0) && (rec.age <= 10)) || (rec.age == 0) ) ) { 
                	cidVal = rec.cid 
        			cidData = rec.data
                	cidReadingAge = rec.age
                	if(rec.units != null)
                    	cidUnit = rec.units
        			break 
     			}
            	//if (rec.age >= 30 && rec.age <= 300) { 
                	//logWriter("Warning!!! There appears to be some sort of delay.  The last reading was received ${rec.age} seconds ago...")
                    //refresh()  /* If time exceeds 30 seconds try to initiate a refresh.  */
                //}
		}
            
 		//Convert data: values to individual strings
		for (item in cidData[0]) {
     		timeVal =  item.key
    		cidReading = item.value
		}
            
        //Converts timeVal string to long integer
        def longTimeVal = timeVal.toLong()

		//Save Cid Type to device state
		state.cidType = cidVal
            
        //Save Cid Unit to device state
        state.cidUnit = cidUnit
            
        //Save last Cid reading value to device state
        state.powerReading = cidReading
            
        //Formats epoch time to Human DateTime Format
        readingUpdated = "${tf.format(longTimeVal)}"
            
        state.energyReading = cidReading.toInteger() / 1000
        //state.powerVal = cidReading
        state.readingUpdated = "Last Updated: ${readingUpdated}"
            
		//Show Debug logging if enabled in preferences
        logWriter(" ")	
        logWriter("-------------------USAGE READING DATA-------------------")
        //logWriter("HTTP Status Response: " + respData)	/*<------Uncomment this line to log the Http response */
		logWriter("Cid Type: " + state.cidType)
        logWriter("Cid Unit: " + cidUnit)
        logWriter("Timestamp: " + timeVal)
        logWriter("reading: " + cidReading)
        logWriter("Last Updated: " + readingUpdated)
        logWriter("Reading Age: " + cidReadingAge)
        logWriter("Current Month: ${state.monthName}")
        logWriter("Day of Week: ${state.dayOfWeek}")
            
    }
	def summaryParams = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile_proxy/getCurrentValuesSummary",
        query: ["token": state.efergyAuthToken],
        contentType: "json"
    	]
	httpGet(summaryParams, summaryClosure)
}

// Returns Hub Device Status Info 
def getHubData() {
	def hubId = ""
    def hubMacAddr = ""
    def hubStatus = ""
    def hubTsHuman
    def hubType = ""
    def hubVersion = ""
    def statusList
    def getStatusClosure = { statusResp ->  
        	def respData = statusResp.data.text
            //Converts http response data to list
			statusList = new JsonSlurper().parseText(respData)
			
           	hubId = statusList.hid
            hubMacAddr = statusList.listOfMacs.mac
    		hubStatus = statusList.listOfMacs.status
    		hubTsHuman = statusList.listOfMacs.tsHuman
    		hubType = statusList.listOfMacs.type
    		hubVersion = statusList.listOfMacs.version
            
            //Save info to device state store
            state.hubId = hubId
            state.hubMacAddr = hubMacAddr.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubStatus = hubStatus.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubTsHuman = hubTsHuman.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubType = hubType.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubVersion = hubVersion.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubName = getHubName(hubType)
			
            //Show Debug logging if enabled in preferences
            logWriter(" ")	
            logWriter("-------------------HUB DEVICE DATA-------------------")
            //logWriter("HTTP Status Response: " + respData)
            logWriter("Hub ID: " + state.hubId)
            logWriter("Hub Mac: " + state.hubMacAddr)
            logWriter("Hub Status: " + state.hubStatus)
            logWriter("Hub TimeStamp: " + state.hubTsHuman)
            logWriter("Hub Type: " + state.hubType)
            logWriter("Hub Firmware: " + state.hubVersion)
            logWriter("Hub Name: " + state.hubName)
    }
    //Http Get Parameters     
	def statusParams = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile_proxy/getStatus",
        query: ["token": state.efergyAuthToken],
        contentType: 'json'
    ]
	httpGet(statusParams, getStatusClosure)
}    

//Log Writer that all logs are channel through *It will only output these if Debug Logging is enabled under preferences
private def logWriter(value) {
	if (state.showLogging) {
        log.debug "${value}"
    }	
}

/******************************************************************************  
*				Application Help and License Info Variables					  *
*******************************************************************************/
private def textAppName() 	{ def text = "${appName()}" }	
private def textVersion() 	{ def text = "Version: ${appVersion()}" }
private def textModified() 	{ def text = "Updated: ${versionDate()}" }
private def textAuthor() 	{ def text = "${appAuthor()}" }
private def textNamespace() { def text = "${appNamespace()}" }
private def textVerInfo() { def text = "${appVerInfo()}" }
private def textCopyright() { def text = "Copyright© 2015 - Anthony S." }
private def textDesc() { def text = "${appDesc()}" }

