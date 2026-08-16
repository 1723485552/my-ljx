import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/plugin/base_tool_plugin.dart';

/// 槽位承载内容类型
enum SlotType { empty, text, image }

/// 独立槽位数据实体 (纯内存驻留)
class MemorySlot {
  MemorySlot({
    required this.id,
    required this.name,
    this.type = SlotType.empty,
    this.textContent = '',
    this.imageBytes,
    this.originUri,
    this.fileName,
    this.totalCountdown = 30,
    this.remainingSeconds = 0,
  });

  final String id;
  String name;
  SlotType type;
  String textContent;
  Uint8List? imageBytes;
  String? originUri;
  String? fileName;

  int totalCountdown;
  int remainingSeconds;
  Timer? timer;

  bool get isCountingDown => remainingSeconds > 0 && timer != null && timer!.isActive;

  void startTimer(VoidCallback onTick, VoidCallback onExpired) {
    timer?.cancel();
    remainingSeconds = totalCountdown;
    timer = Timer.periodic(const Duration(seconds: 1), (Timer t) {
      if (remainingSeconds > 1) {
        remainingSeconds--;
        onTick();
      } else {
        t.cancel();
        remainingSeconds = 0;
        onExpired();
      }
    });
  }

  void stopTimer() {
    timer?.cancel();
    timer = null;
    remainingSeconds = 0;
  }

  void clear() {
    stopTimer();
    type = SlotType.empty;
    textContent = '';
    imageBytes = null;
    originUri = null;
    fileName = null;
  }
}

class TemporaryMemoryPlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'temporary_memory',
        version: '3.0.0',
        name: '瞬时暂存',
        description: '多槽位图文暂存，支持截屏智能载入与倒计时相册自毁',
        category: '文本与效率',
        icon: Icons.timer_outlined,
      );

  @override
  Widget buildView(BuildContext context) {
    return const TemporaryMemoryHubView();
  }

  @override
  void dispose() {
    // 宿主级销毁钩子
  }
}

class TemporaryMemoryHubView extends StatefulWidget {
  const TemporaryMemoryHubView({super.key});

  @override
  State<TemporaryMemoryHubView> createState() => _TemporaryMemoryHubViewState();
}

class _TemporaryMemoryHubViewState extends State<TemporaryMemoryHubView>
    with WidgetsBindingObserver {
  static const MethodChannel _mediaChannel =
      MethodChannel('com.novatoolbox/media_cleaner');

  final List<MemorySlot> _slots = <MemorySlot>[
    MemorySlot(id: 'slot_1', name: '槽位 1'),
    MemorySlot(id: 'slot_2', name: '槽位 2'),
  ];
  int _activeSlotIndex = 0;
  String? _lastLoadedUri; // 避免重复自动载入同一张图片

  bool _watcherActive = false; // 全局悬浮自毁监听是否开启

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // 进入页面时立即静默检查
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _autoCheckRecentScreenshot();
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // 当用户在外部截完屏，重新切回本 App 前台时自动触发
    if (state == AppLifecycleState.resumed) {
      _autoCheckRecentScreenshot();
    }
  }

  MemorySlot get _currentSlot => _slots[_activeSlotIndex];

  // 静默自动检测截图（无感装填）
  Future<void> _autoCheckRecentScreenshot() async {
    try {
      final dynamic result =
          await _mediaChannel.invokeMethod<dynamic>('getLatestScreenshot');
      if (result is Map && mounted) {
        final Map<dynamic, dynamic> map = result;
        final String uri = map['uri'] as String;
        final bool isRecent = map['isRecent'] as bool? ?? false;

        // 如果是 3 分钟内的新截图，且尚未被当前载入过，则全自动装填
        if (isRecent && _lastLoadedUri != uri) {
          _lastLoadedUri = uri;

          setState(() {
            _currentSlot.type = SlotType.image;
            _currentSlot.imageBytes = map['bytes'] as Uint8List;
            _currentSlot.originUri = uri;
            _currentSlot.fileName = map['name'] as String?;
          });

          _startAutoDestroyCountdown();
          _showToast('⚡ 已自动捕获最新截图，自毁倒计时已开启');
        }
      }
    } on PlatformException catch (e) {
      if (e.code == 'PERMISSION_DENIED') {
        _showToast('请授予相册权限以启用截屏自动捕获');
      }
    } on Object {
      // 静默失败，不阻塞主流程
    }
  }

  // 手动载入最新系统截图
  Future<void> _fetchLatestScreenshot() async {
    try {
      final dynamic result =
          await _mediaChannel.invokeMethod<dynamic>('getLatestScreenshot');
      if (result is Map && mounted) {
        final Map<dynamic, dynamic> map = result;
        final String uri = map['uri'] as String;
        _lastLoadedUri = uri;
        setState(() {
          _currentSlot.type = SlotType.image;
          _currentSlot.imageBytes = map['bytes'] as Uint8List;
          _currentSlot.originUri = uri;
          _currentSlot.fileName = map['name'] as String?;
        });

        _startAutoDestroyCountdown();
        _showToast('已载入最新截图，自毁倒计时已开启');
      } else {
        _showToast('未检测到最新截图');
      }
    } on PlatformException catch (e) {
      _showToast('读取相册失败: ${e.message}');
    }
  }

  void _startAutoDestroyCountdown() {
    if (_currentSlot.totalCountdown <= 0) return;
    _currentSlot.startTimer(
      () {
        if (mounted) setState(() {});
      },
      () => _triggerPhotoDeletion(),
    );
  }

  void _setCountdown(int seconds) {
    if (!mounted) return;
    setState(() {
      _currentSlot.totalCountdown = seconds;
      if (_currentSlot.isCountingDown) {
        _currentSlot.stopTimer();
        _startAutoDestroyCountdown();
      }
    });
  }

  // 触发删除原相册图片并清空当前槽位
  Future<void> _triggerPhotoDeletion() async {
    final String? uri = _currentSlot.originUri;
    if (uri != null) {
      try {
        final bool? success = await _mediaChannel.invokeMethod<bool>(
          'deleteMediaUri',
          <String, dynamic>{'uri': uri},
        );

        if (mounted) {
          if (success == true) {
            _showToast('已自毁并移出系统相册');
          } else {
            _showToast('已取消相册删除');
          }
        }
      } on Object catch (e) {
        if (mounted) _showToast('删除相册图片异常: $e');
      }
    }

    if (mounted) {
      setState(() {
        _currentSlot.clear();
      });
    }
  }

  // 全局悬浮自毁监听开关
  Future<void> _toggleGlobalWatcher(bool enable) async {
    if (enable) {
      // 1. 检查悬浮窗权限
      final bool? granted = await _mediaChannel
          .invokeMethod<bool>('checkOverlayPermission');
      if (granted != true) {
        // 2. 未授权则拉起系统设置页
        await _mediaChannel.invokeMethod<bool>('requestOverlayPermission');
        _showToast('请授予「显示在其他应用上层」权限后重试');
        return;
      }

      // 3. 权限具备，启动前台监听服务
      await _mediaChannel.invokeMethod<bool>('startGlobalWatcher');
      if (mounted) {
        setState(() {
          _watcherActive = true;
        });
        _showToast('已开启全局截屏自毁守护');
      }
    } else {
      await _mediaChannel.invokeMethod<bool>('stopGlobalWatcher');
      if (mounted) {
        setState(() => _watcherActive = false);
        _showToast('已关闭全局截屏自毁守护');
      }
    }
  }

  void _showToast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        duration: const Duration(milliseconds: 1200),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    for (final MemorySlot slot in _slots) {
      slot.stopTimer();
      slot.clear();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('瞬时暂存工作台'),
        actions: <Widget>[
          IconButton(
            icon: const Icon(Icons.add_box_outlined),
            tooltip: '新增槽位',
            onPressed: () {
              setState(() {
                final int idx = _slots.length + 1;
                _slots.add(
                  MemorySlot(
                    id: 'slot_${DateTime.now().millisecondsSinceEpoch}',
                    name: '槽位 $idx',
                  ),
                );
                _activeSlotIndex = _slots.length - 1;
              });
            },
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline_rounded),
            tooltip: '清空槽位',
            onPressed: () {
              setState(() => _currentSlot.clear());
              _showToast('已重置槽位');
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: <Widget>[
          // 全局截屏自毁守护开关
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              border: Border(bottom: BorderSide(color: theme.colorScheme.outline)),
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.shield_outlined,
                  size: 20,
                  color: _watcherActive
                      ? theme.colorScheme.primary
                      : theme.colorScheme.outline,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      const Text(
                        '全局截屏自毁守护',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        _watcherActive
                            ? '已开启 · 任意界面截屏将弹出自毁胶囊'
                            : '关闭 · 仅本页手动/自动载入',
                        style: TextStyle(
                          fontSize: 11,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Switch(
                  value: _watcherActive,
                  onChanged: (bool value) => _toggleGlobalWatcher(value),
                ),
              ],
            ),
          ),

          // 槽位选择栏
          Container(
            height: 52,
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              border: Border(bottom: BorderSide(color: theme.colorScheme.outline)),
            ),
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              scrollDirection: Axis.horizontal,
              itemCount: _slots.length,
              separatorBuilder: (BuildContext ctx, int i) => const SizedBox(width: 8),
              itemBuilder: (BuildContext ctx, int i) {
                final MemorySlot slot = _slots[i];
                return ChoiceChip(
                  label: Text(
                    '${slot.name} ${slot.isCountingDown ? "(${slot.remainingSeconds}s)" : ""}',
                  ),
                  selected: i == _activeSlotIndex,
                  onSelected: (bool selected) {
                    if (selected) setState(() => _activeSlotIndex = i);
                  },
                );
              },
            ),
          ),

          // 主视图
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: _buildMainSlotView(theme),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMainSlotView(ThemeData theme) {
    if (_currentSlot.type == SlotType.empty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Icon(Icons.auto_delete_outlined, size: 52, color: theme.colorScheme.outline),
            const SizedBox(height: 12),
            Text(
              '${_currentSlot.name} 准备就绪',
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 6),
            Text(
              '截屏后一键载入，倒计时结束自动调起相册销毁',
              style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 24),
            FilledButton.tonalIcon(
              onPressed: _fetchLatestScreenshot,
              icon: const Icon(Icons.screenshot_monitor_rounded, size: 18),
              label: const Text('抓取最新截图并开启自毁'),
            ),
          ],
        ),
      );
    }

    if (_currentSlot.type == SlotType.image) {
      final double progress = _currentSlot.totalCountdown > 0
          ? _currentSlot.remainingSeconds / _currentSlot.totalCountdown
          : 0.0;

      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: theme.colorScheme.outline),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            // 倒计时指示条
            if (_currentSlot.isCountingDown) ...<Widget>[
              Row(
                children: <Widget>[
                  Icon(Icons.timer_outlined, size: 16, color: theme.colorScheme.error),
                  const SizedBox(width: 6),
                  Text(
                    '相册自毁倒计时: ${_currentSlot.remainingSeconds} 秒',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: theme.colorScheme.error,
                    ),
                  ),
                  const Spacer(),
                  TextButton(
                    style: TextButton.styleFrom(visualDensity: VisualDensity.compact),
                    onPressed: () {
                      setState(() => _currentSlot.stopTimer());
                      _showToast('已取消自动销毁');
                    },
                    child: const Text('取消自毁'),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              LinearProgressIndicator(
                value: progress,
                color: theme.colorScheme.error,
                backgroundColor: theme.colorScheme.error.withValues(alpha: 0.12),
              ),
              const SizedBox(height: 12),
            ],

            // 倒计时档位选择
            Row(
              children: <Widget>[
                const Text('自毁时长:', style: TextStyle(fontSize: 12)),
                const SizedBox(width: 8),
                ChoiceChip(
                  visualDensity: VisualDensity.compact,
                  label: const Text('30秒', style: TextStyle(fontSize: 12)),
                  selected: _currentSlot.totalCountdown == 30,
                  onSelected: (bool s) => _setCountdown(30),
                ),
                const SizedBox(width: 6),
                ChoiceChip(
                  visualDensity: VisualDensity.compact,
                  label: const Text('1分钟', style: TextStyle(fontSize: 12)),
                  selected: _currentSlot.totalCountdown == 60,
                  onSelected: (bool s) => _setCountdown(60),
                ),
                const SizedBox(width: 6),
                ChoiceChip(
                  visualDensity: VisualDensity.compact,
                  label: const Text('3分钟', style: TextStyle(fontSize: 12)),
                  selected: _currentSlot.totalCountdown == 180,
                  onSelected: (bool s) => _setCountdown(180),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // 图像渲染 (保留 LayoutBuilder 约束防御无界溢出)
            Expanded(
              child: LayoutBuilder(
                builder: (BuildContext ctx, BoxConstraints constraints) {
                  return Container(
                    alignment: Alignment.center,
                    constraints: BoxConstraints(
                      maxHeight: constraints.maxHeight,
                      maxWidth: constraints.maxWidth,
                    ),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: _currentSlot.imageBytes != null
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.memory(
                              _currentSlot.imageBytes!,
                              fit: BoxFit.contain,
                              errorBuilder: (BuildContext c, Object err, StackTrace? stack) =>
                                  const Icon(Icons.broken_image_outlined, size: 48),
                            ),
                          )
                        : const Icon(Icons.image_outlined, size: 48),
                  );
                },
              ),
            ),
            const SizedBox(height: 12),

            // 底部操作栏
            Row(
              children: <Widget>[
                Chip(
                  visualDensity: VisualDensity.compact,
                  label: Text(_currentSlot.fileName ?? '截图原件'),
                ),
                const Spacer(),
                FilledButton.icon(
                  style: FilledButton.styleFrom(backgroundColor: theme.colorScheme.error),
                  onPressed: _triggerPhotoDeletion,
                  icon: const Icon(Icons.delete_forever_rounded, size: 16),
                  label: const Text('立即自毁'),
                ),
              ],
            ),
          ],
        ),
      );
    }

    return const SizedBox.shrink();
  }
}
