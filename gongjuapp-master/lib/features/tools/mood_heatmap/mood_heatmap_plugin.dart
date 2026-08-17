import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import '../../../core/plugin/base_tool_plugin.dart';

// 条件导入：Web 浏览器端使用 dart:html 实现海报下载；
// 非 Web 平台 (Android/iOS/桌面) 使用空壳占位（运行时被 kIsWeb 守卫隔离，不会执行）。
import 'dart:html' as html if (dart.library.io) 'html_stub.dart';

class MoodType {
  final int id;
  final String label;
  final Color color;
  final IconData icon;

  const MoodType({
    required this.id,
    required this.label,
    required this.color,
    required this.icon,
  });
}

const List<MoodType> kAllMoods = <MoodType>[
  MoodType(id: 0, label: '能量充实', color: Color(0xFF10B981), icon: Icons.bolt_rounded),
  MoodType(id: 1, label: '平静专注', color: Color(0xFF38BDF8), icon: Icons.spa_rounded),
  MoodType(id: 2, label: '灵感突破', color: Color(0xFFA855F7), icon: Icons.lightbulb_rounded),
  MoodType(id: 3, label: '焦虑疲惫', color: Color(0xFFFBBF24), icon: Icons.grain_rounded),
  MoodType(id: 4, label: '放空自省', color: Color(0xFF71717A), icon: Icons.nights_stay_rounded),
];

class MoodRecord {
  final int moodId;
  final String note;
  const MoodRecord({required this.moodId, required this.note});
}

class MoodHeatmapPlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'mood_heatmap',
        version: '1.0.0',
        name: '365 情绪热力图',
        description: 'GitHub 风格年度心绪像素方阵与生活热力图，支持即时点选与海报导出',
        category: '常用与效率',
        icon: Icons.grid_view_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const MoodHeatmapView();
  }

  @override
  void dispose() {}
}

class MoodHeatmapView extends StatefulWidget {
  const MoodHeatmapView({super.key});

  @override
  State<MoodHeatmapView> createState() => _MoodHeatmapViewState();
}

class _MoodHeatmapViewState extends State<MoodHeatmapView> {
  final GlobalKey _posterBoundaryKey = GlobalKey();
  final int _currentYear = 2026;
  final Map<String, MoodRecord> _records = <String, MoodRecord>{};
  late final DateTime _firstDayOfYear;
  late final int _totalDays;

  int _selectedFilterMood = -1; // -1 表示全部展示

  @override
  void initState() {
    super.initState();
    _firstDayOfYear = DateTime(_currentYear, 1, 1);
    final bool isLeapYear =
        (_currentYear % 4 == 0 && _currentYear % 100 != 0) ||
            (_currentYear % 400 == 0);
    _totalDays = isLeapYear ? 366 : 365;
    _populateSampleData();
  }

  // 注入预置热力数据以便初次进入即可看到精美热力阵列
  void _populateSampleData() {
    final DateTime now = DateTime.now();
    final int daysSinceYearStart = now.difference(_firstDayOfYear).inDays;
    final int pastDays = daysSinceYearStart.clamp(30, 260);

    for (int i = 0; i < pastDays; i++) {
      if (i % 5 == 0) continue; // 模拟偶发的未记录状态
      final DateTime date = _firstDayOfYear.add(Duration(days: i));
      final String key = _formatDateKey(date);
      final int moodId = (i * 7 + (i % 3)) % 5;
      _records[key] = MoodRecord(
        moodId: moodId,
        note: i % 4 == 0 ? '专注推进核心里程碑' : '日常复盘与思考',
      );
    }
  }

  String _formatDateKey(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}';
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

  void _openMoodLogger(DateTime date) {
    final String key = _formatDateKey(date);
    final MoodRecord? current = _records[key];
    int selectedMood = current?.moodId ?? 0;
    final TextEditingController noteCtrl =
        TextEditingController(text: current?.note ?? '');

    showModalBottomSheet<void>(
      context: context,
      backgroundColor: const Color(0xFF18181B),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (BuildContext ctx) {
        return StatefulBuilder(
          builder: (BuildContext context,
              void Function(void Function()) setSheetState) {
            return Padding(
              padding: EdgeInsets.fromLTRB(20, 16, 20,
                  MediaQuery.of(context).viewInsets.bottom + 20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Text(
                        key,
                        style: const TextStyle(
                            color: Color(0xFFF4F4F5),
                            fontSize: 16,
                            fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        '心绪定格',
                        style: TextStyle(color: Color(0xFF71717A), fontSize: 13),
                      ),
                      const Spacer(),
                      if (current != null)
                        TextButton(
                          onPressed: () {
                            setState(() => _records.remove(key));
                            Navigator.pop(ctx);
                          },
                          child: const Text('清除',
                              style: TextStyle(color: Color(0xFFEF4444), fontSize: 12)),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: kAllMoods.map((MoodType m) {
                      final bool isSelected = selectedMood == m.id;
                      return GestureDetector(
                        onTap: () => setSheetState(() => selectedMood = m.id),
                        child: Column(
                          children: <Widget>[
                            AnimatedContainer(
                              duration: const Duration(milliseconds: 200),
                              width: 44,
                              height: 44,
                              decoration: BoxDecoration(
                                color: isSelected ? m.color : const Color(0xFF27272A),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: isSelected ? Colors.white : Colors.transparent,
                                  width: 2,
                                ),
                              ),
                              child: Icon(
                                m.icon,
                                color: isSelected
                                    ? const Color(0xFF09090B)
                                    : m.color,
                                size: 22,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              m.label,
                              style: TextStyle(
                                color: isSelected
                                    ? Colors.white
                                    : const Color(0xFFA1A1AA),
                                fontSize: 10,
                                fontWeight: isSelected
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: const Color(0xFF27272A),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: TextField(
                      controller: noteCtrl,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                      maxLength: 20,
                      decoration: const InputDecoration(
                        hintText: '今日一句话备忘 (选填，最多20字)...',
                        hintStyle: TextStyle(color: Color(0xFF71717A), fontSize: 12),
                        border: InputBorder.none,
                        counterText: '',
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    height: 44,
                    child: FilledButton(
                      style: FilledButton.styleFrom(
                        backgroundColor: const Color(0xFFF4F4F5),
                        foregroundColor: const Color(0xFF09090B),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: () {
                        setState(() {
                          _records[key] = MoodRecord(
                            moodId: selectedMood,
                            note: noteCtrl.text.trim(),
                          );
                        });
                        Navigator.pop(ctx);
                      },
                      child: const Text('确认保存心绪',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  // 生成并导出高清年度心绪海报
  Future<void> _exportPoster() async {
    try {
      final RenderRepaintBoundary? boundary = _posterBoundaryKey
          .currentContext?.findRenderObject() as RenderRepaintBoundary?;
      if (boundary == null) return;

      final ui.Image image = await boundary.toImage(pixelRatio: 3.0);
      final ByteData? byteData =
          await image.toByteData(format: ui.ImageByteFormat.png);
      if (byteData == null) return;
      final Uint8List pngBytes = byteData.buffer.asUint8List();

      if (kIsWeb) {
        final html.Blob blob =
            html.Blob(<dynamic>[pngBytes], 'image/png');
        final String url = html.Url.createObjectUrlFromBlob(blob);
        final html.AnchorElement anchor = html.AnchorElement(href: url)
          ..setAttribute('download', 'Mood_Heatmap_$_currentYear.png')
          ..click();
        html.Url.revokeObjectUrl(url);
        _showToast('年度心绪海报已开始下载');
      } else {
        _showToast('海报渲染完成 (已载入内存)');
      }
    } on Object catch (e) {
      _showToast('海报导出失败: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    const Color bgDark = Color(0xFF09090B);
    const Color surfaceDark = Color(0xFF18181B);
    const Color borderDark = Color(0xFF27272A);
    const Color textPrimary = Color(0xFFF4F4F5);
    const Color textSecondary = Color(0xFFA1A1AA);

    // 统计计算
    final int recordedCount = _records.length;
    final double completionRate =
        (_totalDays > 0) ? (recordedCount / _totalDays) * 100 : 0.0;

    final Map<int, int> moodCounts = <int, int>{0: 0, 1: 0, 2: 0, 3: 0, 4: 0};
    for (final MoodRecord r in _records.values) {
      moodCounts[r.moodId] = (moodCounts[r.moodId] ?? 0) + 1;
    }

    return Scaffold(
      backgroundColor: bgDark,
      appBar: AppBar(
        backgroundColor: bgDark,
        elevation: 0,
        title: Text(
          '$_currentYear MOOD MATRIX',
          style: const TextStyle(
            color: textPrimary,
            fontSize: 13,
            letterSpacing: 2.0,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
        actions: <Widget>[
          IconButton(
            icon: const Icon(Icons.share_rounded, color: textPrimary, size: 20),
            tooltip: '导出年度热力海报',
            onPressed: _exportPoster,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: <Widget>[
          // 顶部全景概览卡片
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: borderDark),
            ),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      const Text('已记录天数',
                          style: TextStyle(color: textSecondary, fontSize: 11)),
                      const SizedBox(height: 4),
                      Text(
                        '$recordedCount / $_totalDays 天',
                        style: const TextStyle(
                            color: textPrimary,
                            fontSize: 18,
                            fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ),
                Container(width: 1, height: 32, color: borderDark),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      const Text('年度心绪覆盖率',
                          style: TextStyle(color: textSecondary, fontSize: 11)),
                      const SizedBox(height: 4),
                      Text(
                        '${completionRate.toStringAsFixed(1)}%',
                        style: const TextStyle(
                            color: Color(0xFF10B981),
                            fontSize: 18,
                            fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // 情绪过滤 Chip 条
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: FilterChip(
                    label: const Text('全部'),
                    selected: _selectedFilterMood == -1,
                    labelStyle: TextStyle(
                      fontSize: 11,
                      color: _selectedFilterMood == -1
                          ? const Color(0xFF09090B)
                          : textSecondary,
                      fontWeight: _selectedFilterMood == -1
                          ? FontWeight.bold
                          : FontWeight.normal,
                    ),
                    selectedColor: const Color(0xFFF4F4F5),
                    backgroundColor: surfaceDark,
                    side: BorderSide(
                        color: _selectedFilterMood == -1
                            ? Colors.transparent
                            : borderDark),
                    onSelected: (_) => setState(() => _selectedFilterMood = -1),
                  ),
                ),
                ...kAllMoods.map((MoodType m) {
                  final bool isSelected = _selectedFilterMood == m.id;
                  final int count = moodCounts[m.id] ?? 0;
                  return Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: FilterChip(
                      avatar: CircleAvatar(backgroundColor: m.color, radius: 4),
                      label: Text('${m.label} ($count)'),
                      selected: isSelected,
                      labelStyle: TextStyle(
                        fontSize: 11,
                        color: isSelected
                            ? const Color(0xFF09090B)
                            : textSecondary,
                        fontWeight: isSelected
                            ? FontWeight.bold
                            : FontWeight.normal,
                      ),
                      selectedColor: m.color,
                      backgroundColor: surfaceDark,
                      side: BorderSide(
                          color: isSelected ? Colors.transparent : borderDark),
                      onSelected: (_) => setState(() =>
                          _selectedFilterMood = isSelected ? -1 : m.id),
                    ),
                  );
                }),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // 热力海报容器（用于导出）
          RepaintBoundary(
            key: _posterBoundaryKey,
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFF101012),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: borderDark),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(Icons.hub_rounded,
                          color: Color(0xFF38BDF8), size: 16),
                      const SizedBox(width: 6),
                      Text(
                        '$_currentYear 365 像素方阵',
                        style: const TextStyle(
                            color: textPrimary,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 0.5),
                      ),
                      const Spacer(),
                      const Text(
                        '点击方块定格心绪',
                        style: TextStyle(color: Color(0xFF71717A), fontSize: 10),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // GitHub 风格 53 周 × 7 天热力方阵
                  _buildHeatmapGrid(),
                  const SizedBox(height: 14),

                  // 底部图例
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: <Widget>[
                      const Text('未记录',
                          style: TextStyle(color: Color(0xFF71717A), fontSize: 9)),
                      const SizedBox(width: 4),
                      _buildLegendBox(const Color(0xFF1F1F23)),
                      const SizedBox(width: 4),
                      ...kAllMoods.map((MoodType m) => Padding(
                            padding: const EdgeInsets.only(left: 4),
                            child: _buildLegendBox(m.color),
                          )),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),

          // 今日速记快捷入口
          SizedBox(
            height: 46,
            child: FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFF4F4F5),
                foregroundColor: const Color(0xFF09090B),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: () => _openMoodLogger(DateTime.now()),
              icon: const Icon(Icons.edit_calendar_rounded, size: 18),
              label: const Text('定格今日心绪',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildLegendBox(Color c) {
    return Container(
      width: 10,
      height: 10,
      decoration: BoxDecoration(
        color: c,
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }

  Widget _buildHeatmapGrid() {
    // 2026年1月1日为星期四 (weekday = 4)
    final int startWeekdayOffset = _firstDayOfYear.weekday - 1; // 0 = 周一
    final int totalBlocks = startWeekdayOffset + _totalDays;
    final int totalCols = (totalBlocks / 7).ceil();

    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final double availableWidth = constraints.maxWidth;
        // 计算每个像素方块的自适应边长
        final double boxSize =
            ((availableWidth - (totalCols - 1) * 2.5) / totalCols)
                .clamp(4.0, 10.0);

        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List<Widget>.generate(totalCols, (int col) {
              return Padding(
                padding: const EdgeInsets.only(right: 2.5),
                child: Column(
                  children: List<Widget>.generate(7, (int row) {
                    final int dayIndex =
                        col * 7 + row - startWeekdayOffset;
                    if (dayIndex < 0 || dayIndex >= _totalDays) {
                      return SizedBox(width: boxSize, height: boxSize + 2.5);
                    }

                    final DateTime date =
                        _firstDayOfYear.add(Duration(days: dayIndex));
                    final String key = _formatDateKey(date);
                    final MoodRecord? rec = _records[key];

                    Color cellColor = const Color(0xFF1F1F23);
                    if (rec != null) {
                      if (_selectedFilterMood == -1 ||
                          _selectedFilterMood == rec.moodId) {
                        cellColor = kAllMoods[rec.moodId].color;
                      } else {
                        cellColor = const Color(0xFF18181B);
                      }
                    }

                    final bool isToday =
                        _formatDateKey(DateTime.now()) == key;

                    return Padding(
                      padding: const EdgeInsets.only(bottom: 2.5),
                      child: Tooltip(
                        message:
                            '$key ${rec != null ? "(${kAllMoods[rec.moodId].label}) ${rec.note}" : "(未记录)"}',
                        child: GestureDetector(
                          onTap: () => _openMoodLogger(date),
                          child: Container(
                            width: boxSize,
                            height: boxSize,
                            decoration: BoxDecoration(
                              color: cellColor,
                              borderRadius: BorderRadius.circular(2),
                              border: isToday
                                  ? Border.all(color: Colors.white, width: 1)
                                  : null,
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
                ),
              );
            }),
          ),
        );
      },
    );
  }
}
