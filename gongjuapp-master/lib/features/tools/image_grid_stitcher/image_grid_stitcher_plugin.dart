import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/plugin/base_tool_plugin.dart';

enum GridPreset {
  grid3x3('九宫格 (3×3)', 3, 3),
  grid2x2('四宫格 (2×2)', 2, 2),
  grid3x1('横向三联 (3×1)', 1, 3),
  grid1x3('纵向三联 (1×3)', 3, 1);

  final String label;
  final int rows;
  final int cols;
  const GridPreset(this.label, this.rows, this.cols);
}

enum SquareMode {
  padWhite('等比留白 (推荐)', '自动补全白色底板至1:1，完整保留文字与全景'),
  centerCrop('居中裁剪 (1:1)', '裁取中心最大正方形区域后切分'),
  original('原比例直切', '按原图比例切分，不进行1:1规范化');

  final String label;
  final String hint;
  const SquareMode(this.label, this.hint);
}

class ImageGridStitcherPlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'image_grid_stitcher',
        version: '1.1.0',
        name: '九宫切图与拼长图',
        description: '纯内存0广告多宫格切图与长图无缝拼接，即用即走零相册残留',
        category: '图片与多媒体',
        icon: Icons.grid_on_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const ImageGridStitcherView();
  }

  @override
  void dispose() {}
}

class ImageGridStitcherView extends StatefulWidget {
  const ImageGridStitcherView({super.key});

  @override
  State<ImageGridStitcherView> createState() => _ImageGridStitcherViewState();
}

class _ImageGridStitcherViewState extends State<ImageGridStitcherView> with SingleTickerProviderStateMixin {
  static const MethodChannel _mediaChannel = MethodChannel('com.novatoolbox/native_media');

  late final TabController _tabController;

  // --- 切图状态 ---
  Uint8List? _sourceImageBytes;
  GridPreset _selectedPreset = GridPreset.grid3x3;
  SquareMode _selectedSquareMode = SquareMode.padWhite;
  List<Uint8List> _slicedCaches = <Uint8List>[];
  bool _isProcessingSplit = false;

  // --- 拼接状态 ---
  final List<Uint8List> _stitchQueue = <Uint8List>[];
  double _stitchSpacing = 0.0;
  Uint8List? _stitchedResultBytes;
  bool _isProcessingStitch = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _sourceImageBytes = null;
    _slicedCaches.clear();
    _stitchQueue.clear();
    _stitchedResultBytes = null;
    super.dispose();
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

  // ---------------- 核心像素渲染管线 (dart:ui) ----------------
  Future<ui.Image> _decodeImage(Uint8List bytes) async {
    final Completer<ui.Image> completer = Completer<ui.Image>();
    ui.decodeImageFromList(bytes, (ui.Image img) => completer.complete(img));
    return completer.future;
  }

  // 1. 1:1 规范化多宫格切割
  Future<void> _processGridSlice() async {
    if (_sourceImageBytes == null) return;
    setState(() => _isProcessingSplit = true);

    try {
      final ui.Image src = await _decodeImage(_sourceImageBytes!);
      ui.Image workingImg = src;

      // 1:1 规范化预处理
      if (_selectedSquareMode == SquareMode.padWhite) {
        final int maxSide = src.width > src.height ? src.width : src.height;
        final ui.PictureRecorder recorder = ui.PictureRecorder();
        final ui.Canvas canvas = ui.Canvas(recorder);

        // 绘制白色底板
        canvas.drawRect(
          Rect.fromLTWH(0, 0, maxSide.toDouble(), maxSide.toDouble()),
          Paint()..color = const Color(0xFFFFFFFF),
        );

        // 居中绘制原图
        final double offsetX = (maxSide - src.width) / 2.0;
        final double offsetY = (maxSide - src.height) / 2.0;
        canvas.drawImage(src, Offset(offsetX, offsetY), Paint());

        final ui.Picture picture = recorder.endRecording();
        workingImg = await picture.toImage(maxSide, maxSide);
      } else if (_selectedSquareMode == SquareMode.centerCrop) {
        final int minSide = src.width < src.height ? src.width : src.height;
        final double cropX = (src.width - minSide) / 2.0;
        final double cropY = (src.height - minSide) / 2.0;

        final ui.PictureRecorder recorder = ui.PictureRecorder();
        final ui.Canvas canvas = ui.Canvas(recorder);

        canvas.drawImageRect(
          src,
          Rect.fromLTWH(cropX, cropY, minSide.toDouble(), minSide.toDouble()),
          Rect.fromLTWH(0, 0, minSide.toDouble(), minSide.toDouble()),
          Paint(),
        );

        final ui.Picture picture = recorder.endRecording();
        workingImg = await picture.toImage(minSide, minSide);
      }

      // 执行网格均分
      final int rows = _selectedPreset.rows;
      final int cols = _selectedPreset.cols;
      final double cellWidth = workingImg.width / cols;
      final double cellHeight = workingImg.height / rows;

      final List<Uint8List> result = <Uint8List>[];

      for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
          final ui.PictureRecorder recorder = ui.PictureRecorder();
          final ui.Canvas canvas = ui.Canvas(recorder);

          final Rect srcRect = Rect.fromLTWH(c * cellWidth, r * cellHeight, cellWidth, cellHeight);
          final Rect dstRect = Rect.fromLTWH(0, 0, cellWidth, cellHeight);

          canvas.drawImageRect(workingImg, srcRect, dstRect, Paint());
          final ui.Picture picture = recorder.endRecording();
          final ui.Image slice = await picture.toImage(cellWidth.toInt(), cellHeight.toInt());
          final ByteData? data = await slice.toByteData(format: ui.ImageByteFormat.png);
          if (data != null) {
            result.add(data.buffer.asUint8List());
          }
        }
      }

      if (mounted) {
        setState(() {
          _slicedCaches = result;
          _isProcessingSplit = false;
        });
        _showToast('切图完成 (${_slicedCaches.length} 张)');
      }
    } on Object catch (e) {
      if (mounted) {
        setState(() => _isProcessingSplit = false);
        _showToast('切图异常: $e');
      }
    }
  }

  // 2. 长图无缝拼接
  Future<void> _processStitch() async {
    if (_stitchQueue.length < 2) {
      _showToast('请至少添加 2 张图片');
      return;
    }

    setState(() => _isProcessingStitch = true);

    try {
      final List<ui.Image> decodedList = <ui.Image>[];
      for (final Uint8List b in _stitchQueue) {
        decodedList.add(await _decodeImage(b));
      }

      int maxWidth = 0;
      for (final ui.Image img in decodedList) {
        if (img.width > maxWidth) maxWidth = img.width;
      }
      if (maxWidth == 0) maxWidth = 1080;

      double totalHeight = 0;
      final List<double> scaledHeights = <double>[];
      for (final ui.Image img in decodedList) {
        final double scale = maxWidth / img.width;
        final double h = img.height * scale;
        scaledHeights.add(h);
        totalHeight += h;
      }
      totalHeight += _stitchSpacing * (decodedList.length - 1);

      final ui.PictureRecorder recorder = ui.PictureRecorder();
      final ui.Canvas canvas = ui.Canvas(recorder);

      canvas.drawRect(
        Rect.fromLTWH(0, 0, maxWidth.toDouble(), totalHeight),
        Paint()..color = const Color(0xFFFFFFFF),
      );

      double currentY = 0;
      for (int i = 0; i < decodedList.length; i++) {
        final ui.Image img = decodedList[i];
        final double h = scaledHeights[i];
        final Rect srcRect = Rect.fromLTWH(0, 0, img.width.toDouble(), img.height.toDouble());
        final Rect dstRect = Rect.fromLTWH(0, currentY, maxWidth.toDouble(), h);

        canvas.drawImageRect(img, srcRect, dstRect, Paint());
        currentY += h + _stitchSpacing;
      }

      final ui.Picture picture = recorder.endRecording();
      final ui.Image finalImage = await picture.toImage(maxWidth, totalHeight.toInt());
      final ByteData? data = await finalImage.toByteData(format: ui.ImageByteFormat.png);

      if (mounted) {
        setState(() {
          _stitchedResultBytes = data?.buffer.asUint8List();
          _isProcessingStitch = false;
        });
        _showToast('长图拼接完成');
      }
    } on Object catch (e) {
      if (mounted) {
        setState(() => _isProcessingStitch = false);
        _showToast('拼接异常: $e');
      }
    }
  }

  // ---------------- 选图交互 ----------------
  Future<void> _pickImageForGrid() async {
    try {
      final Uint8List? imageBytes = await _mediaChannel.invokeMethod<Uint8List>('pickImage');
      if (imageBytes != null && mounted) {
        setState(() {
          _sourceImageBytes = imageBytes;
          _slicedCaches.clear();
        });
        _processGridSlice();
      }
    } on Object catch (e) {
      _showToast('选择图片失败: $e');
    }
  }

  Future<void> _pickImageForStitch() async {
    try {
      final Uint8List? imageBytes = await _mediaChannel.invokeMethod<Uint8List>('pickImage');
      if (imageBytes != null && mounted) {
        setState(() {
          _stitchQueue.add(imageBytes);
          _stitchedResultBytes = null;
        });
        _showToast('已追加第 ${_stitchQueue.length} 张图片');
      }
    } on Object catch (e) {
      _showToast('选择图片失败: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('九宫切图与拼长图'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const <Widget>[
            Tab(icon: Icon(Icons.grid_3x3_rounded, size: 20), text: '多格切图'),
            Tab(icon: Icon(Icons.view_stream_rounded, size: 20), text: '长图无缝拼接'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: <Widget>[
          _buildGridSplitterTab(theme),
          _buildStitcherTab(theme),
        ],
      ),
    );
  }

  // ---------------- Tab 1: 多格切图 ----------------
  Widget _buildGridSplitterTab(ThemeData theme) {
    if (_sourceImageBytes == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Icon(Icons.add_photo_alternate_outlined, size: 56, color: theme.colorScheme.outline),
            const SizedBox(height: 16),
            const Text('载入图片进行多宫格切分', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            Text('纯内存切图，不向相册写入碎图垃圾', style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 24),
            FilledButton.tonalIcon(
              onPressed: _pickImageForGrid,
              icon: const Icon(Icons.photo_library_outlined),
              label: const Text('选择图片'),
            ),
          ],
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        // 画幅规范化选择
        Text('画幅适配模式:', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: theme.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 6),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: SquareMode.values.map((SquareMode mode) {
              final bool isSelected = _selectedSquareMode == mode;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(mode.label),
                  selected: isSelected,
                  onSelected: (bool selected) {
                    if (selected) {
                      setState(() => _selectedSquareMode = mode);
                      _processGridSlice();
                    }
                  },
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 12),

        // 分割规格选择
        Text('切割宫格:', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: theme.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 6),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: GridPreset.values.map((GridPreset preset) {
              final bool isSelected = _selectedPreset == preset;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(preset.label),
                  selected: isSelected,
                  onSelected: (bool selected) {
                    if (selected) {
                      setState(() => _selectedPreset = preset);
                      _processGridSlice();
                    }
                  },
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 16),

        // 切割结果交互式网格
        if (_isProcessingSplit)
          const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()))
        else if (_slicedCaches.isNotEmpty)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: theme.colorScheme.outline),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Text(
                      '发帖预览 (完整无裁切)',
                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: theme.colorScheme.primary),
                    ),
                    const Spacer(),
                    TextButton.icon(
                      onPressed: _pickImageForGrid,
                      icon: const Icon(Icons.refresh_rounded, size: 16),
                      label: const Text('更换图片'),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _slicedCaches.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: _selectedPreset.cols,
                    crossAxisSpacing: 6,
                    mainAxisSpacing: 6,
                  ),
                  itemBuilder: (BuildContext ctx, int index) {
                    return Container(
                      decoration: BoxDecoration(
                        color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Stack(
                        fit: StackFit.expand,
                        children: <Widget>[
                          ClipRRect(
                            borderRadius: BorderRadius.circular(6),
                            child: Image.memory(
                              _slicedCaches[index],
                              fit: BoxFit.contain,
                            ),
                          ),
                          Positioned(
                            top: 4,
                            left: 4,
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                              decoration: BoxDecoration(
                                color: Colors.black.withValues(alpha: 0.65),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                '${index + 1}',
                                style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
      ],
    );
  }

  // ---------------- Tab 2: 长图无缝拼接 ----------------
  Widget _buildStitcherTab(ThemeData theme) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        Row(
          children: <Widget>[
            FilledButton.tonalIcon(
              onPressed: _pickImageForStitch,
              icon: const Icon(Icons.add_photo_alternate_outlined, size: 18),
              label: const Text('添加图片'),
            ),
            const SizedBox(width: 8),
            if (_stitchQueue.isNotEmpty)
              OutlinedButton(
                onPressed: () => setState(() {
                  _stitchQueue.clear();
                  _stitchedResultBytes = null;
                }),
                child: const Text('清空'),
              ),
            const Spacer(),
            if (_stitchQueue.length >= 2)
              FilledButton.icon(
                onPressed: _isProcessingStitch ? null : _processStitch,
                icon: const Icon(Icons.auto_awesome_rounded, size: 16),
                label: const Text('开始拼接'),
              ),
          ],
        ),
        const SizedBox(height: 16),

        if (_stitchQueue.isNotEmpty) ...<Widget>[
          Row(
            children: <Widget>[
              const Text('拼接间距:', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
              const SizedBox(width: 8),
              SegmentedButton<double>(
                segments: const <ButtonSegment<double>>[
                  ButtonSegment<double>(value: 0.0, label: Text('0px 无缝')),
                  ButtonSegment<double>(value: 8.0, label: Text('8px')),
                  ButtonSegment<double>(value: 16.0, label: Text('16px')),
                ],
                selected: <double>{_stitchSpacing},
                onSelectionChanged: (Set<double> val) {
                  setState(() => _stitchSpacing = val.first);
                  if (_stitchedResultBytes != null) _processStitch();
                },
              ),
            ],
          ),
          const SizedBox(height: 16),
        ],

        if (_stitchQueue.isNotEmpty && _stitchedResultBytes == null)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: theme.colorScheme.outline),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text('待拼接序列 (已选 ${_stitchQueue.length} 张):', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                const SizedBox(height: 10),
                ReorderableListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _stitchQueue.length,
                  onReorder: (int oldIndex, int newIndex) {
                    setState(() {
                      if (oldIndex < newIndex) newIndex -= 1;
                      final Uint8List item = _stitchQueue.removeAt(oldIndex);
                      _stitchQueue.insert(newIndex, item);
                    });
                  },
                  itemBuilder: (BuildContext ctx, int i) {
                    return ListTile(
                      key: ValueKey<int>(i),
                      leading: ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: Image.memory(_stitchQueue[i], width: 44, height: 44, fit: BoxFit.cover),
                      ),
                      title: Text('图片 #${i + 1}', style: const TextStyle(fontSize: 13)),
                      trailing: IconButton(
                        icon: const Icon(Icons.close_rounded, size: 18),
                        onPressed: () => setState(() => _stitchQueue.removeAt(i)),
                      ),
                    );
                  },
                ),
              ],
            ),
          ),

        if (_stitchedResultBytes != null) ...<Widget>[
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: theme.colorScheme.outline),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Text('拼接长图预览', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: theme.colorScheme.primary)),
                    const Spacer(),
                    Chip(
                      visualDensity: VisualDensity.compact,
                      label: Text('${(_stitchedResultBytes!.lengthInBytes / 1024).toStringAsFixed(1)} KB'),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.memory(_stitchedResultBytes!, fit: BoxFit.contain),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}
