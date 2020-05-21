var xmlHttpRequest = new XMLHttpRequest();

function getSensorStatus() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=null", true);
	xmlHttpRequest.onreadystatechange = printSensorStatus;
	xmlHttpRequest.send(null);
}
function printSensorStatus() {
	if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
		var dom = (new DOMParser()).parseFromString(
				xmlHttpRequest.responseText, "text/xml");

		// Show or hide buttons
		// offen
		if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("close").style.display = 'block';
			document.getElementById("stop").style.display = 'none';
			document.getElementById("gate").className = "alert alert-danger";
			document.getElementById("gate").innerHTML = "Tor ist offen";

			// halb geöffnet
		} else if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "2"
				&& dom.getElementsByTagName("moving")[0].childNodes[0].nodeValue == "0") {
			document.getElementById("close").style.display = 'block';
			document.getElementById("stop").style.display = 'none';
			document.getElementById("gate").className = "alert alert-info";
			document.getElementById("gate").innerHTML = "Tor halb offen";

			// Flügel hat sich von selber bewegt
		} else if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "3"
				&& dom.getElementsByTagName("moving")[0].childNodes[0].nodeValue == "0") {
			document.getElementById("close").style.display = 'block';
			document.getElementById("stop").style.display = 'none';
			document.getElementById("gate").className = "alert alert-info";
			document.getElementById("gate").innerHTML = "Fl&uuml;gel hat sich bewegt, vermute offen";

			// öffnet gerade
		} else if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "2"
				&& dom.getElementsByTagName("moving")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("close").style.display = 'none';
			document.getElementById("stop").style.display = 'block';
			document.getElementById("stopButton").value = "Tor Anhalten";
			document.getElementById("gate").className = "alert alert-info";
			document.getElementById("gate").innerHTML = "Tor &ouml;ffnet gerade";

			// schließt gerade
		} else if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "3"
				&& dom.getElementsByTagName("moving")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("close").style.display = 'none';
			document.getElementById("stop").style.display = 'block';
			document.getElementById("stopButton").value = "Schließen abbrechen, öffnen";
			document.getElementById("gate").className = "alert alert-info";
			document.getElementById("gate").innerHTML = "Tor schlie&szlig;t gerade";

			// Zu
		} else if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "0") {
			document.getElementById("close").style.display = 'none';
			document.getElementById("stop").style.display = 'none';
			document.getElementById("gate").className = "alert alert-success";
			document.getElementById("gate").innerHTML = "Tor ist zu";
		}

		// Lock und Open Anzeigen wenn zu / gesperrt
		if (dom.getElementsByTagName("gate")[0].childNodes[0].nodeValue == "0") {
			if (dom.getElementsByTagName("locked")[0].childNodes[0].nodeValue == "0") {
				document.getElementById("open").style.display = 'block';
				document.getElementById("unlock").style.display = 'none';
				document.getElementById("lock").style.display = 'block';
			} else {
				document.getElementById("unlock").style.display = 'block';
				document.getElementById("lock").style.display = 'none';
				document.getElementById("open").style.display = 'none';
			}
		} else {
			document.getElementById("open").style.display = 'none';
			document.getElementById("lock").style.display = 'none';
			document.getElementById("unlock").style.display = 'none';
		}

		// Kill-Auto wenn Aktiv
		if (dom.getElementsByTagName("auto")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("kill-auto").style.display = 'block';
		} else {
			document.getElementById("kill-auto").style.display = 'none';
		}

		// Assign labels & colors to status elements
		if (dom.getElementsByTagName("door")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("door").className = "alert alert-danger";
			document.getElementById("door").innerHTML = "T&uuml;r ist offen";
		} else if (dom.getElementsByTagName("door")[0].childNodes[0].nodeValue == "0") {
			document.getElementById("door").className = "alert alert-success";
			document.getElementById("door").innerHTML = "T&uuml;r ist zu";
		}
		if (dom.getElementsByTagName("lb")[0].childNodes[0].nodeValue == "1") {
			document.getElementById("lb").className = "alert alert-danger";
			document.getElementById("lb").innerHTML = "Lichtschranke ist unterbrochen";
		} else if (dom.getElementsByTagName("lb")[0].childNodes[0].nodeValue == "0") {
			document.getElementById("lb").className = "alert alert-success";
			document.getElementById("lb").innerHTML = "Lichtschranke ist geschlossen";
		}

		// Print temperature
		document.getElementById("temp").innerHTML = ("Au&szlig;entemperatur: " + (dom
				.getElementsByTagName("temp"))[0].childNodes[0].nodeValue);

		// Print info
		document.getElementById("info").innerHTML = dom
				.getElementsByTagName("status")[0].childNodes[0].nodeValue;

	}
}

getSensorStatus();
setInterval(function() {
	getSensorStatus();
}, 1000);

function closeDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=closeDoor", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function openDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=openDoor", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function openAutoCloseDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=openAutoCloseDoor", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function stopDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=stopDoor", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function lockDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=lock&state=true", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function unlockDoor() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=lock&state=false", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function killAuto() {
	xmlHttpRequest.open("Get", "/Hoftor/Webui?action=kill-auto", true);
	xmlHttpRequest.onreadystatechange = printInfo;
	xmlHttpRequest.send(null);
}

function printInfo() {
	if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
		var xml = (new DOMParser()).parseFromString(
				xmlHttpRequest.responseText, "text/xml");
		var info = document.getElementById("info").innerHTML
				+ (xml.getElementsByTagName("status"))[0].childNodes[0].nodeValue
				+ "</br>";
		document.getElementById("info").innerHTML = info;

	}
}