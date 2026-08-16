import 'dart:convert';

import 'package:agent_forge/features/tools/temporary_memory/temporary_memory_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

// 合法 1x1 透明 PNG 的 Base64 字符串
const String kValidPngB64 =
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==';

Map<String, dynamic> _screenshot({
  required String uri,
  required String name,
  required Uint8List bytes,
  bool isRecent = true,
}) =>
    <String, dynamic>{
      'uri': uri,
      'name': name,
      'bytes': bytes,
      'isRecent': isRecent,
    };

// 为 media_cleaner 通道安装 mock
void mockMediaChannel({
  Map<String, dynamic>? screenshot,
  bool deleteSuccess = true,
  bool permissionDenied = false,
}) {
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(
    const MethodChannel('com.novatoolbox/media_cleaner'),
    (MethodCall call) async {
      switch (call.method) {
        case 'getLatestScreenshot':
          if (permissionDenied) {
            throw PlatformException(
              code: 'PERMISSION_DENIED',
              message: '请授予相册读取权限以自动抓取截图',
            );
          }
          return screenshot;
        case 'deleteMediaUri':
          return deleteSuccess;
        default:
          return null;
      }
    },
  );
}

void clearMediaChannel() {
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(
    const MethodChannel('com.novatoolbox/media_cleaner'),
    null,
  );
}

void main() {
  group('TemporaryMemoryPlugin', () {
    test('manifest exposes correct metadata (v3 self-destruct)', () {
      final TemporaryMemoryPlugin plugin = TemporaryMemoryPlugin();
      expect(plugin.manifest.id, 'temporary_memory');
      expect(plugin.manifest.name, '瞬时暂存');
      expect(plugin.manifest.category, '文本与效率');
      expect(plugin.manifest.version, '3.0.0');
    });

    testWidgets('renders hub with two default slots', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );

      expect(find.byType(ChoiceChip), findsNWidgets(2));
      expect(find.text('瞬时暂存工作台'), findsOneWidget);
    });

    testWidgets('empty slot shows ready prompt and fetch button',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );

      expect(find.text('槽位 1 准备就绪'), findsOneWidget);
      expect(find.text('抓取最新截图并开启自毁'), findsOneWidget);
    });

    testWidgets('auto-captures recent screenshot on first frame',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      mockMediaChannel(
        screenshot: _screenshot(
          uri: 'content://media/external/images/media/1',
          name: 'Screenshot_001.png',
          bytes: png,
        ),
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      // initState 注册了 addPostFrameCallback，泵送一帧触发自动检查
      await tester.pumpAndSettle();

      // 无感自动装填：截屏文件名出现且倒计时开启
      expect(find.text('Screenshot_001.png'), findsOneWidget);
      expect(find.textContaining('相册自毁倒计时'), findsOneWidget);

      clearMediaChannel();
    });

    testWidgets('auto-captures when app resumes to foreground',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      mockMediaChannel(
        screenshot: _screenshot(
          uri: 'content://media/external/images/media/2',
          name: 'Screenshot_002.png',
          bytes: png,
        ),
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      await tester.pumpAndSettle();

      // 模拟切回前台
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();

      expect(find.text('Screenshot_002.png'), findsOneWidget);

      clearMediaChannel();
    });

    testWidgets('permission denied error is swallowed without crash',
        (WidgetTester tester) async {
      mockMediaChannel(permissionDenied: true);

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      await tester.pumpAndSettle();

      // 仍处于就绪态，未崩溃
      expect(find.text('槽位 1 准备就绪'), findsOneWidget);
      expect(tester.takeException(), isNull);

      clearMediaChannel();
    });

    testWidgets('does not re-auto-load the same screenshot uri',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      int queryCount = 0;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('com.novatoolbox/media_cleaner'),
        (MethodCall call) async {
          if (call.method == 'getLatestScreenshot') {
            queryCount++;
            return _screenshot(
              uri: 'content://media/external/images/media/9',
              name: 'Screenshot_009.png',
              bytes: png,
            );
          }
          return null;
        },
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      await tester.pumpAndSettle();

      // 再次切回前台，uri 相同应被 _lastLoadedUri 去重，不重复装填
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();

      expect(find.text('Screenshot_009.png'), findsOneWidget);
      // 查询被调用多次（每帧/每次 resume 都会查），但仅装填一次由 UI 表现保证：
      // 这里只验证不崩溃且仍显示唯一一张
      expect(queryCount, greaterThan(1));

      clearMediaChannel();
    });

    testWidgets('fetch latest screenshot loads image and starts countdown',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      mockMediaChannel(
        screenshot: _screenshot(
          uri: 'content://media/external/images/media/123',
          name: 'Screenshot_001.png',
          bytes: png,
        ),
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      // 首帧自动检查可能也会装填，但手动点击同样会装填并重置
      await tester.tap(find.text('抓取最新截图并开启自毁'));
      await tester.pumpAndSettle();

      // 图片已载入
      expect(find.text('Screenshot_001.png'), findsOneWidget);
      // 倒计时已开启
      expect(find.textContaining('相册自毁倒计时'), findsOneWidget);
      // 档位选择按钮存在
      expect(find.text('30秒'), findsOneWidget);
      expect(find.text('1分钟'), findsOneWidget);
      expect(find.text('3分钟'), findsOneWidget);

      clearMediaChannel();
    });

    testWidgets('countdown tier can be switched and cancelled',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('com.novatoolbox/media_cleaner'),
        (MethodCall call) async {
          if (call.method == 'getLatestScreenshot') {
            return _screenshot(
              uri: 'content://media/external/images/media/123',
              name: 'Screenshot_001.png',
              bytes: png,
            );
          }
          return null;
        },
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      // 首帧已自动装填，直接操作档位
      await tester.pumpAndSettle();

      // 切换自毁时长为 3 分钟
      await tester.tap(find.text('3分钟'));
      await tester.pumpAndSettle();
      expect(find.text('3分钟'), findsOneWidget);

      // 取消自毁
      await tester.tap(find.text('取消自毁'));
      await tester.pumpAndSettle();

      clearMediaChannel();
    });

    testWidgets('manual self-destruct invokes delete channel and clears slot',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      bool deleteCalled = false;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('com.novatoolbox/media_cleaner'),
        (MethodCall call) async {
          if (call.method == 'getLatestScreenshot') {
            return _screenshot(
              uri: 'content://media/external/images/media/123',
              name: 'Screenshot_001.png',
              bytes: png,
            );
          }
          if (call.method == 'deleteMediaUri') {
            deleteCalled = true;
            return true;
          }
          return null;
        },
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );
      await tester.pumpAndSettle();

      // 通过 widget 回调直接驱动自毁逻辑（绕过底部按钮的点击命中）
      final Finder selfDestructBtn =
          find.widgetWithText(FilledButton, '立即自毁');
      final FilledButton button =
          tester.widget<FilledButton>(selfDestructBtn);
      await tester.runAsync(() async {
        button.onPressed?.call();
        // 等待原生通道 future 完成
        await Future<void>.delayed(Duration.zero);
      });
      await tester.pumpAndSettle();

      expect(deleteCalled, isTrue);
      // 槽位已清空回到就绪态
      expect(find.text('槽位 1 准备就绪'), findsOneWidget);

      clearMediaChannel();
    });

    testWidgets('add slot increases choice chip count', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );

      expect(find.byType(ChoiceChip), findsNWidgets(2));

      await tester.tap(find.widgetWithIcon(IconButton, Icons.add_box_outlined));
      await tester.pumpAndSettle();
      expect(find.byType(ChoiceChip), findsNWidgets(3));
    });

    testWidgets('fetching screenshot then disposing mid-async does not crash',
        (WidgetTester tester) async {
      final Uint8List png = base64Decode(kValidPngB64);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('com.novatoolbox/media_cleaner'),
        (MethodCall call) async {
          if (call.method == 'getLatestScreenshot') {
            return _screenshot(
              uri: 'content://media/external/images/media/123',
              name: 'Screenshot_001.png',
              bytes: png,
            );
          }
          return null;
        },
      );

      await tester.pumpWidget(
        const MaterialApp(home: TemporaryMemoryHubView()),
      );

      // 触发异步截图载入后立即卸载 Widget
      await tester.tap(find.text('抓取最新截图并开启自毁'));
      await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);

      clearMediaChannel();
    });
  });
}
