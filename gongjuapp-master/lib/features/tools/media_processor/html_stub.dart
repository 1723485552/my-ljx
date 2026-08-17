// 条件导入占位：非 Web 平台（Android / iOS / 桌面）使用该空壳。
// 运行时所有 html.* 引用都被 kIsWeb 守卫包裹、不会执行，此处仅保证静态编译通过。
import 'dart:async';
import 'dart:typed_data';

class Event {}

class File {
  final String name;
  File(this.name);
}

class FileUploadInputElement {
  String? accept;
  List<File>? files;
  Stream<Event> get onChange => const Stream.empty();
  void click() {}
}

class FileReader {
  dynamic result;
  Stream<Event> get onLoadEnd => const Stream.empty();
  void readAsArrayBuffer(File file) {}
}

class Blob {
  // ignore: unused_element
  Blob(List<dynamic> data, String type);
}

class Url {
  static String createObjectUrlFromBlob(Blob blob) => '';
  static void revokeObjectUrl(String url) {}
}

class AnchorElement {
  String? href;
  AnchorElement({String? href});
  void setAttribute(String name, String value) {}
  void click() {}
}

// Web Audio 相关占位（仅编译期需要，运行时不会进入）
class AudioBuffer {
  int get numberOfChannels => 2;
  double get sampleRate => 44100;
  int get length => 0;
  double get duration => 0;
  Float32List getChannelData(int channel) => Float32List(0);
}

class AudioContext {
  Future<AudioBuffer> decodeAudioData(
    ByteBuffer audioData, [
    void Function(AudioBuffer)? successCallback,
    void Function? errorCallback,
  ]) async =>
      throw UnimplementedError('Web Audio 仅在 Web 平台可用');
}
