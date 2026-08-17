// 条件导入占位：非 Web 平台（Android / iOS / 桌面）使用该空壳。
// 运行时所有 html.* 引用都被 kIsWeb 守卫包裹、不会执行，此处仅保证静态编译通过。
import 'dart:async';

class Event {}

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
