if (window.File && window.FileReader && window.FileList && window.Blob) {
  // Great success! All the File APIs are supported.
} else {
  alert('The File APIs are not fully supported in this browser.');
}

function handleFileSelect(evt) {
	var file = evt.target.files[0];
	
	var filereader = new FileReader();
	
	//TODO some fancy loader swirly 
	//setup the callback used when loading the file from disk
	filereader.onload = (function(e) {
		//convert the string into a JSON array of arrays
		var sampletabstring = stringToJSON2DArray(e.target.result)
		
		//do the ajax call
	    $.ajax({
           type:           'POST',
           url:            'api/jsac',
           contentType:    'application/json',
           data:           sampletabstring,
           processData:    false,
           success: function(json) {
        	   //once the ajax call is complete, display the output
        	   //through this callback
        	   doResponse(json.errors, json.sampletab)
           },
           error: function(request, error_type, error_mesg) {
        	   //if the ajax call when awry, tell the user
               alert('Oops! Something went wrong whilst trying to display your results.\n' +
                             'A script on this page said...\n' +
                             '\"' + error_type + ': ' + error_mesg + '\"');
           }
       });
	})
	//now setup is complete, actually read the file
	filereader.readAsText(file, "UTF-8");
}

function doResponse(errors, sampletab) {
    alert(errors);
    //This uses Downloadify from https://github.com/dcneiner/Downloadify
    //Note this requires JS + Flash.
    $("#sampletabdiv").downloadify( filename="sampletab.txt", data=sampletab );
}

function stringToJSON2DArray(myString) {
    var content = new Array();
    var lines = myString.split("\n");
    for (var i = 0; i<lines.length; i++) {
        var line = new Array();
        var cells = lines[i].split("\t");
        for (var j = 0; j < cells.length; j++) {
            line.push("\"" + cells[j] + "\"");
        }
        content.push("[" + line + "]");
    }
    return "{\"sampletab\" : [" + content + "]}";
}

$(document).ready(function() {
	document.getElementById('pickfile').addEventListener('change', handleFileSelect, false);
	});