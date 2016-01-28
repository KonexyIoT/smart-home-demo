<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Konexy Demo WebApp</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <link href="/assets/css/bootstrap.min.css" rel="stylesheet">
    <link href="/assets/css/docs.min.css" rel="stylesheet">
    <link href="/assets/css/bootstrap-toggle.min.css" rel="stylesheet">

    <script src="/assets/js/jquery-1.11.3.min.js"></script>
    <script src="/assets/js/bootstrap.min.js"></script>
    <script src="/assets/js/bootstrap-toggle.min.js"></script>
    <script src="/assets/js/websocket.js"></script>
    <script src="/assets/js/Highcharts-4.1.10/js/highcharts.js"></script>
    <script src="/assets/js/Highcharts-4.1.10/js/modules/exporting.js"></script>
    <script src="/assets/js/Highcharts-4.1.10/js/themes/sand-signika.js"></script>
</head>
<body>
    <div class="container">
        <div id="header" class="row">
            <h1>Konexy Demo</h1>
        </div>

        <div id="updateStateModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="updateStateModal">
            <div class="modal-dialog" role="document">
                <div class="alert alert-success" role="alert">
                    <p id="updateState"></p>
                </div>
            </div>
        </div>

        <div id="finishUpdateDeviceStatusModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="finishUpdateDeviceStatusModal">
            <div class="modal-dialog" role="document">
                <div class="alert alert-success" role="alert">
                    <p>Đã cập nhật các trạng thái của thiết bị</p>
                </div>
            </div>
        </div>

        <div class="row">
            <div id="default-warning" class="alert alert-success">
                <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                <span>Chúc mừng, các thiết bị của bạn đang hoạt động bình thường</span>
            </div>
            <div id="danger-warning" class="alert alert-danger" style="display: none;">
                <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                <span>Cảnh báo:</span><span id="alert-message"></span>
            </div>

            <div class="bs-callout bs-callout-info" id="callout-navs-tabs-plugin">
                <h4>Welcome to the Konexy Home.</h4>
                <p>Konexy là một Platform thiết kế theo mô hình PaaS phục vụ cho IoT và các Dự án M2M.</p>
            </div>

            <div class="col-md-4">
                <div class="panel panel-info">
                    <div class="panel-heading">Thông tin thiết bị</div>
                    <div class="panel-body">
                        <h5 class="font-bold">Meta Data</h5>
                        <ul class="list-group text-xs">
                            <li class="list-group-item">Ngày tạo <span class="pull-right">2015/12/14 15:59:25</span></li>
                        </ul>
                        <h5 class="font-bold">Thuộc tính</h5>
                        <pre><code>{"Device":"Lumi","Description":"De mo bo tap trung"}</code></pre>
                    </div>
                </div>
            </div>

            <div class="col-md-4">
                <div class="panel panel-info">
                    <div class="panel-heading">Điều khiển</div>
                    <div class="panel-body">
                        <div class="form-group col-md-6">
                            <label>Light 1: </label>
                            <input id="device-1" class="device-state" sub-id="1" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                        <div class="form-group">
                            <label>Light 2: </label>
                            <input id="device-2" class="device-state" sub-id="2" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                        <div class="form-group col-md-6">
                            <label>Light 3: </label>
                            <input id="device-3" class="device-state" sub-id="3" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                        <div class="form-group">
                            <label>Light 4: </label>
                            <input id="device-4" class="device-state" sub-id="4" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                        <div class="form-group col-md-6">
                            <label>Light 5: </label>
                            <input id="device-5" class="device-state" sub-id="5" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                        <div class="form-group">
                            <label>Light 6: </label>
                            <input id="device-6" class="device-state" sub-id="6" type="checkbox" checked disabled data-toggle="toggle">
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="temperatureChart" class="row"></div>

        <script>
            $(function () {
                $(document).ready(function () {
                    Highcharts.setOptions({
                        global: {
                            useUTC: false
                        }
                    });

                    $('#temperatureChart').highcharts({
                        chart: {
                            type: 'spline',
                            animation: Highcharts.svg, // don't animate in old IE
                            marginRight: 10
                        },
                        title: {
                            text: 'Biểu đồ nhiệt độ thiết bị: Demo'
                        },
                        xAxis: {
                            type: 'datetime',
                            tickPixelInterval: 150
                        },
                        yAxis: {
                            title: {
                                text: 'Nhiệt độ'
                            },
                            plotLines: [{
                                value: 0,
                                width: 1,
                                color: '#808080'
                            }]
                        },
                        tooltip: {
                            formatter: function () {
                                return '<b>' + this.series.name + '</b><br/>' +
                                        'Time: ' + Highcharts.dateFormat('%H:%M:%S', this.x) + '<br/>' +
                                        'Nhiệt độ: ' + Highcharts.numberFormat(this.y) + "°C";
                            }
                        },
                        legend: {
                            enabled: false
                        },
                        exporting: {
                            enabled: false
                        },
                        series: [{
                            name: 'Nhiệt kế Demo',
                            data: []
                        }]
                    });
                });

                $('.device-state').change(function() {
                    var no = parseInt($(this).attr("sub-id"));
                    if (initLock[no]||updateLock[no]) {
                        updateLock[no] = false;
                        return;
                    }

                    var state = ($(this).prop('checked') == true) ? "on" : "off";
                    publishLightState(no, state);
                });

                // Device state
                var warningTime = setInterval(function (){
                    $('#default-warning').attr("style", "display: none;");
                    clearInterval(warningTime);
                }, 2000);
            });
        </script>
    </div>
</body>
</html>