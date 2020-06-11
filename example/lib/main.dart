import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:aquera_input_mic/aquera_input_mic.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_email_sender/flutter_email_sender.dart';
import 'package:path_provider/path_provider.dart';
import 'package:screen_keep_on/screen_keep_on.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    SystemChrome.setPreferredOrientations(
        [DeviceOrientation.portraitUp, DeviceOrientation.portraitDown]);
    return MaterialApp(
      title: 'Aquera Mic Reader',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Aquera Microphone Test'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<FrequencyPoint> measuredData;
  Stream<FrequencyPoint> micData;
  StreamSubscription<FrequencyPoint> listener;
  List<double> _fftBuffer;
  List<FrequencyPoint> points;
  List<int> _samplesBuffer;
  bool applySquareRoot;
  double noiseLevel;
  int noiseSamples;
  int _sampleFrequency;
  int _fftPoints;
  int _bufferSize;
  int _numberOfBins;
  double _freqBin;
  double _binOffset;
  double _speedFactor;
  AudioSource _audioSource;
  bool _screenStateRecording = false;
  double _noiseMinDb;
  double _noiseMaxDb;
  double _noiseDbSum;

  int measurementsNeeded;

  int get numberOfBins =>
      (_numberOfBins != null) ? _numberOfBins : (fftPoints / 2).floor();
  double get freqBin =>
      (_freqBin != null) ? _freqBin : (sampleFrequency / 2) / numberOfBins;
  double get binOffset => (_binOffset != null) ? _binOffset : (freqBin / 2);
  int get sampleFrequency => _sampleFrequency;
  int get fftPoints => _fftPoints;
  int get bufferSize => _bufferSize;
  int get noiseSampleCount => 10 * sampleFrequency;
  double get speedFactor => _speedFactor;

  set fftPoints(int p) {
    _fftPoints = p;
    _numberOfBins = (_fftPoints / 2).floor();
    _fftBuffer = new List<double>(_fftPoints);
    _fftBuffer.fillRange(0, _fftPoints, 0.0);
  }

  set sampleFrequency(int f) {
    _sampleFrequency = f;
    _freqBin = (sampleFrequency / 2) / numberOfBins;
    _binOffset = freqBin / 2;
    _bufferSize = (sampleFrequency * speedFactor).floor();
  }

  set speedFactor(double f) {
    if (f > 2) {
      f = 2;
    }
    _speedFactor = f;
    _bufferSize = (sampleFrequency * speedFactor).floor();
  }

  double generateSample(int sampleNum) {
    return math.sin(2 * math.pi * 470 / 8000 * sampleNum);
  }

  _MyHomePageState() {
    measurementsNeeded = 485;
    measuredData = new List<FrequencyPoint>();
    points = new List<FrequencyPoint>(5);
    micData = null;
    listener = null;
    applySquareRoot = true;
    _sampleFrequency = 1600;
    _fftPoints = 4096;
    _bufferSize = 800;
    _speedFactor = 1;
    sampleFrequency = 1600;
    fftPoints = 4096;
    noiseLevel = 0;
    noiseSamples = 0;
    _audioSource = AudioSource.UNPROCESSED;
    _samplesBuffer = new List<int>();
    for (var i = 0; i < 5; ++i) {
      points[i] = new FrequencyPoint(0, 0);
    }
  }

  Future<bool> isRecording() async {
    return await AqueraInputMic().recording;
  }

  abs(x) => (x >= 0) ? x : -x;

  void samplesReceived(FrequencyPoint point) {
    double diff = point.frequency - points[4].frequency;
    if (diff < 0) {
      diff = -diff;
    }
    if (noiseSamples < noiseSampleCount) {
      if (noiseSamples == 0) {
        _noiseDbSum = 0;
        _noiseMaxDb = double.negativeInfinity;
        _noiseMinDb = double.infinity;
      }
      print("Samples for noise check: " + noiseSamples.toString());
      _noiseDbSum += point.dbs;
      if (point.dbs > _noiseMaxDb) {
        _noiseMaxDb = point.dbs;
      }
      if (point.dbs < _noiseMinDb) {
        _noiseMinDb = point.dbs;
      }

      noiseSamples += bufferSize;

      if (noiseSamples >= noiseSampleCount) {
        var noisePointsCounter = noiseSamples / bufferSize;
        var noiseAvg = _noiseDbSum / noisePointsCounter;
        var estimate1 = _noiseMinDb - (_noiseMaxDb - _noiseMinDb);
        var estimate2 = noiseAvg - .5 * (noiseAvg - _noiseMinDb);
        print(
            "Calculating noise level: MinDb $_noiseMinDb, MaxDb: $_noiseMaxDb, AVG: $noiseAvg, EST1: $estimate1, EST2: $estimate2");
        noiseLevel = estimate2 > estimate1 ? estimate2 : estimate1;
        print("Noise level set to $noiseLevel");
      }
    } else {
      // filter out anything bellow 15Hz
      // repeated values
      // and anything below noise level
      print("noise level: " +
          noiseLevel.toStringAsFixed(2) +
          "dB;  freq: " +
          point.frequency.toStringAsFixed(2) +
          "Hz " +
          point.dbs.toStringAsFixed(2) +
          'dB');
      if (point.frequency > 15 && diff > 0.0001 && point.dbs >= noiseLevel) {
        for (var i = 0; i < 4; ++i) {
          points[i] = points[i + 1];
        }
        points[4] = point;
        measuredData.add(points[4]);
        if (measuredData.length >= measurementsNeeded) {
          startStop();
        }
      }
    }
    AqueraInputMic().recording.then((bool recording) {
      setState(() {
        if (measuredData.length >= measurementsNeeded) {
          print("Finished...");
        }

        _screenStateRecording = recording;
        ScreenKeepOn.turnOn(recording);
      });
    });
  }

  void sendRecordByEmail(List<FrequencyPoint> data) async {
    Directory docsDir = await getTemporaryDirectory();
    File dataFile = File(docsDir.path + "/voice_data.txt");
    if (dataFile.existsSync()) {
      dataFile.deleteSync();
    }
    var fileContents = "";
    for (FrequencyPoint point in data) {
      fileContents = fileContents +
          point.frequency.toStringAsFixed(12) +
          ';' +
          point.dbs.toStringAsFixed(12) +
          "\n";
    }
    dataFile.writeAsStringSync(fileContents);

    await AqueraInputMic().stop();
    listener.cancel();
    listener = null;

    var now = new DateTime.now();
    var mailto = 'hans@aquera.org';
    var subject = 'voice data from cell phone @' + now.toIso8601String();
    var body =
        'Attached to this e-mail is the text file with the voice recording from the cell phone at ' +
            now.toIso8601String();
    final Email email = Email(
        body: body,
        subject: subject,
        recipients: [mailto],
        attachmentPath: dataFile.path,
        isHTML: false);
    await FlutterEmailSender.send(email);
    await AqueraInputMic().stop();

    AqueraInputMic().recording.then((bool recording) {
      setState(() {
        this.measuredData.clear();
        this.noiseLevel = 0;
        this.noiseSamples = 0;
        print("After sending file, reccording was $recording");
        _screenStateRecording = recording;
      });
    });
  }

  Future<void> _confirmSend(List<FrequencyPoint> data) async {
    return showDialog(
        context: context,
        barrierDismissible: false,
        builder: (BuildContext context) {
          return AlertDialog(
              title: Text("Sending incomplete data"),
              content: SingleChildScrollView(
                  child: ListBody(
                children: [
                  Text(
                      "You will send an imcomplete (less than 480 samples) data file. " +
                          "Are you sure?")
                ],
              )),
              actions: <Widget>[
                Row(
                  children: <Widget>[
                    FlatButton(
                        child: Text("Return"),
                        onPressed: () {
                          Navigator.of(context).pop();
                        }),
                    FlatButton(
                        child: Text("Send anyway"),
                        onPressed: () {
                          sendRecordByEmail(data);
                          Navigator.of(context).pop();
                        })
                  ],
                )
              ]);
        });
  }

  void startStop() async {
    AqueraInputMic micControl = AqueraInputMic();
    bool recording = await isRecording();
    if (recording) {
      print("Recording. Will pause to evaluate the collected data");
      await micControl.pause();
      // discard pending data from the buffer
      _samplesBuffer.clear();
      if (measuredData.length < measurementsNeeded) {
        print("Insufficient data.");
        _confirmSend(measuredData);
      } else {
        print("Recording completed");
        sendRecordByEmail(measuredData);
      }
    } else {
      bool active = await micControl.active;
      if (active) {
        print("Recording is active... resuming it");
        await micControl.resume();
      } else {
        print("Recording is not active... starting it");
        micData = await AqueraInputMic().start(
            audioResolution: AudioResolution.PCM_16BIT,
            sampleRate: sampleFrequency,
            channelConfig: ChannelConfig.MONO,
            audioSource: _audioSource,
            fftBufferSize: _fftPoints,
            bufferSize: bufferSize);
        if (listener == null) {
          listener = micData.listen((samples) {
            samplesReceived(samples);
          });
        }
        bool started = await micControl.recording;
        while (!started) {
          started = await micControl.recording;
        }
      }
    }
    micControl.recording.then((bool recording) {
      setState(() {
        _screenStateRecording = recording;
        ScreenKeepOn.turnOn(recording);
      });
    });
  }

  Widget _parametersForm(BuildContext context) {
    return Container(
        padding: EdgeInsets.all(2),
        child: ListView(
          shrinkWrap: true,
          children: <Widget>[
            ListTile(
              title: Row(children: [
                Text('Sample Rate: '),
                DropdownButton(
                    value: sampleFrequency,
                    items: [
                      DropdownMenuItem(value: 16000, child: new Text("16 KHz")),
                      DropdownMenuItem(value: 8000, child: new Text("8 KHz")),
                      DropdownMenuItem(value: 4000, child: new Text("4 KHz")),
                      DropdownMenuItem(value: 2666, child: new Text("2.6 KHz")),
                      DropdownMenuItem(value: 2000, child: new Text("2 KHz")),
                      DropdownMenuItem(value: 1600, child: new Text("1.6 KHz")),
                      DropdownMenuItem(value: 800, child: new Text("800 Hz"))
                    ],
                    onChanged: (int freq) {
                      setState(() {
                        sampleFrequency = freq;
                      });
                    })
              ]),
            ),
            ListTile(
              title: Row(children: [
                Text('Buffer size: '),
                DropdownButton(
                    value: fftPoints,
                    items: [
                      DropdownMenuItem(value: 32768, child: new Text("32K")),
                      DropdownMenuItem(value: 16384, child: new Text("16K")),
                      DropdownMenuItem(value: 8192, child: new Text("8K")),
                      DropdownMenuItem(value: 4096, child: new Text("4K")),
                      DropdownMenuItem(value: 2048, child: new Text("2K")),
                      DropdownMenuItem(value: 1024, child: new Text("1K")),
                    ],
                    onChanged: (int points) {
                      setState(() {
                        fftPoints = points;
                      });
                    })
              ]),
            ),
            ListTile(
              title: Row(children: [
                Text('Record Size: '),
                DropdownButton(
                    value: measurementsNeeded,
                    items: [
                      DropdownMenuItem(
                          value: 1445, child: new Text("1445 samples")),
                      DropdownMenuItem(
                          value: 965, child: new Text("965 samples")),
                      DropdownMenuItem(
                          value: 485, child: new Text("485 samples")),
                    ],
                    onChanged: (int points) {
                      setState(() {
                        measurementsNeeded = points;
                      });
                    })
              ]),
            ),
            ListTile(
              title: Column(children: [
                Text('Speed Factor:'),
                Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    mainAxisSize: MainAxisSize.max,
                    children: [
                      Expanded(
                          child: Slider(
                        value: speedFactor,
                        min: 0.25,
                        max: 2,
                        onChanged: (double v) {
                          setState(() {
                            speedFactor = v;
                          });
                        },
                      )),
                      Text(speedFactor.toStringAsFixed(2) + "s/FFT")
                    ])
              ]),
            ),
            ListTile(
                title: Row(children: [
              Text('Mic Setup: '),
              DropdownButton(
                  value: _audioSource,
                  items: [
                    DropdownMenuItem(
                        value: AudioSource.UNPROCESSED,
                        child: new Text("Unprocessed (raw)")),
                    DropdownMenuItem(
                        value: AudioSource.MIC,
                        child: new Text("Default Settings")),
                    DropdownMenuItem(
                        value: AudioSource.VOICE_COMMUNICATION,
                        child: new Text("Opt. Voice (AGC)"))
                  ],
                  onChanged: (AudioSource source) {
                    setState(() {
                      _audioSource = source;
                    });
                  })
            ]))
          ],
        ));
  }

  bool get isSamplingNoise => (noiseSamples < noiseSampleCount);

  double get progressIndicator {
    if (isSamplingNoise) {
      return noiseSamples / noiseSampleCount;
    } else {
      return measuredData.length / measurementsNeeded;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_screenStateRecording ? 'Recording...' : widget.title),
      ),
      body: Center(
        child: Column(mainAxisAlignment: MainAxisAlignment.center, children: <
            Widget>[
          new Spacer(),
          new Text((_screenStateRecording && noiseSamples < (noiseSampleCount))
              ? "Measuring environment noise. Please alternate speaking and silence for about 10 seconds."
              : ""),
          new Container(
              padding: EdgeInsets.all(20),
              /*
                child: new LinearProgressIndicator(
                    value: measuredData.length / measurements_needed,
                    valueColor: AlwaysStoppedAnimation<Color>(Colors.red)),
                    */
              child: Center(
                  child: (_screenStateRecording)
                      ? SizedBox(
                          height: 110,
                          width: 110,
                          child: CircularProgressIndicator(
                              value: progressIndicator,
                              backgroundColor: Colors.black12,
                              strokeWidth: 10,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                  isSamplingNoise ? Colors.green : Colors.red)))
                      : _parametersForm(context))),
          new Spacer(),
          new Container(
              padding: EdgeInsets.all(20),
              child: new Text(!_screenStateRecording
                  ? "This is the Aquera VA Mobile microphone test Application. "
                  : "Recording voice. Please speak")),
          new Spacer(),
          new Row(children: [
            new Spacer(),
            new RaisedButton(
                onPressed: startStop,
                child: new Text(_screenStateRecording
                    ? 'Stop'
                    : ((noiseSamples > noiseSampleCount)
                        ? 'Continue'
                        : 'Start'))),
            new Spacer()
          ]),
          new Spacer(),
          new Container(
              padding: EdgeInsets.all(20),
              child: new Text(
                  "Press start then talk continuously with the microphone of the cell phone near your mouth. " +
                      "It will record the voice sample (485 samples) and allow you to send it to the developers to be analyzed.")),
        ]),
      ),
    );
  }
}
