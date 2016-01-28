/**
 * Created by konexy on 12/12/2015.
 */
function randomString(length) {
	var chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    var result = '';
    for (var i = length; i > 0; --i) result += chars[Math.round(Math.random() * (chars.length - 1))];
    return result;
}
var uid = randomString(12, '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ');
var socket = new WebSocket("ws://localhost//message.action?uid=" + uid);
socket.onmessage = onMessage;
socket.onopen = onOpen;
socket.onclose = onClose;
socket.onerror = onError;

var initLock=[];
var updateLock=[];
var controlLock=[];
var controlId=[];
var controlTime=[];
var lastDrawTime=0;

var lastState=[];
var lockerTimer=[];

function onOpen(e) {
    console.log("=================================================");
    console.log("onOpen");
    console.log(e);
    console.log(socket.readyState);
    console.log(socket.extensions);
    console.log("=================================================");
}

function onError(e) {
    console.log("=================================================");
    console.log("onError");
    console.log(e);
    console.log(socket.readyState);
    console.log(socket.extensions);
    console.log("=================================================");
}

function onClose(e) {
    console.log("=================================================");
    console.log("onClose");
    console.log(e);
    console.log(socket.readyState);
    console.log(socket.extensions);
    console.log("=================================================");
}

function onMessage(event) {
    console.log("=================================================");
    console.log(socket.bufferedAmount);
    console.log(event);
    var obj = JSON.parse(event.data);
    console.log("onMessage");
    console.log(obj);

    if (obj.type == "finishUpdateDeviceStatus") {
        console.log("finishUpdateDeviceStatus");
        finishUpdateDeviceStatus();
    }
    if (obj.type == "log") {
        drawTemperature(obj.time*1000, obj.temperature);
    }
    if (obj.type=="update") {
        update(obj.no, obj.state, null);
    }
    if (obj.type=="control") {
        control(obj.no, obj.controlId, obj.time);
        update(obj.no, obj.state, obj.controlId);
    }
    if (obj.type=="ack") {
        update(obj.no, obj.state, obj.controlId);
    }
    if (obj.type=="alert") {
        alertMessage(obj.time, obj.aMessage);
    }
    if (obj.type=="message") {
        messageModal(obj.no, obj.update);
    }
    console.log("=================================================");
}

function drawTemperature(time, temperature) {
    console.log("drawTemperature");
    console.log(time);
    console.log(temperature);
    if(time<=lastDrawTime) {
        console.log("Nhieu time:");
        console.log(time);
        return;
    }

    lastDrawTime = time;

    var chart = $('#temperatureChart').highcharts();
    chart.series[0].addPoint([time, temperature]);
}

function update(no, state, id) {
    console.log("updateLightState");
    console.log("no");
    console.log(no);
    console.log("state");
    console.log(state);
    console.log("id");
    console.log(id);

    if(id!=null)
        lastState[no] = false;

    // @todo
    if(id)
        clearInterval(lockerTimer[no]);

    updateLock[no] = true;
    var controlId = "#device-" + no;
    console.log("controlId");
    console.log(controlId);
    if(typeof(initLock[no])=="undefined"||initLock[no]==true) {
        $(controlId).removeAttr("disabled");
        initLock[no] = false;
    }
    $(controlId).bootstrapToggle(state);
}

function control(no, id, time) {
    controlLock[no] = true;
    controlId[no] = id;
    controlTime[no] = time;
    // @todo
    if(lastState[no])
        lastState[no] = ($(this).prop('checked') == true) ? "on" : "off";
}

function alertMessage(timer, msg) {
    console.log("alertMessage");
    console.log(msg);
    $("#danger-warning").removeAttr("style");
    $('#alert-message').text(msg);

    // blink
    var blinkTimer = setInterval(function (){
        $('#danger-warning').attr("style", "display: none;");
        $('#alert-message').text("");
        clearInterval(blinkTimer);
    }, 5000);
}

function messageModal(no, state) {
    $('#updateStateModal').modal('show');
    $('#updateState').text("Update " + state);
    resetConnectionUnlockTimer(no);
    var waittingClose = setInterval(function (){
        $('#updateStateModal').modal('hide');
        clearInterval(waittingClose);
    }, 1000);
}

function finishUpdateDeviceStatus() {
    $('#finishUpdateDeviceStatusModal').modal('show');
    var waittingClose = setInterval(function (){
        $('#finishUpdateDeviceStatusModal').modal('hide');
        clearInterval(waittingClose);
    }, 1000);
}

function publishLightState(no, state) {
    var action = {
        "sub-device": "light",
        "no": no,
        action: state
    };
    console.log("do publish");
    var message = JSON.stringify(action);
    //socket.send(temp);
    $.post( "/control.action", { "uid": uid, "message": message } );
    // Setting lock
    connectionLockEvent(no);

    // register unlock event
    lockerTimer[no] = setInterval(function(){
        var message = "Your control message sending failure.";
        connectionUnlockEvent(no, message);
    },3000);
}

function connectionLockEvent(no) {
    var controlId = "#device-" + no;
    $(controlId).attr("disabled");
}

function connectionUnlockEvent(no, message) {
    clearInterval(lockerTimer[no]);
    //alert(message);

    var controlId = "#device-" + no;
    $(controlId).removeAttr("disabled");
}

function resetConnectionUnlockTimer(no) {
    // clear
    clearInterval(lockerTimer[no]);
    // re-register
    lockerTimer[no] = setInterval(function(){
        var message = "Connection to device was lost.";
        connectionUnlockEvent(no, message);
    },15000);
}