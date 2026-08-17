import 'dart:async';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/plugin/base_tool_plugin.dart';

enum MaskStyle {
  blackBar('极简黑条', Color(0xFF1E1E1E), false),
  avatarCircle('圆形头像罩', Color(0xFF2B2B2B), true),
  whiteBar('极简白条', Color(0xFFFFFFFF), false),
  subtleGray('优雅浅灰', Color(0xFFE2E2E2), false);

  final String label;
  final Color color;
  final bool isCircle;
  const MaskStyle(this.label, this.color, this.isCircle);
}

class MaskItem {
  final Rect normalizedRect; // 0.0 ~ 1.0 归一化坐标，实现高分辨率原画保真
  final MaskStyle style;

  const MaskItem({required this.normalizedRect, required this.style});
}

class ChatPrivacyMaskerPlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'chat_privacy_masker',
        version: '1.0.0',
        name: '截图隐私遮罩',
        description: '纯内存极简打码脱敏，告别杂乱涂鸦，一键规整遮盖头像、昵称与敏感内容',
        category: '图片与多媒体',
        icon: Icons.security_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const ChatPrivacyMaskerView();
  }

  @override
  void dispose() {}
}

class ChatPrivacyMaskerView extends StatefulWidget {
  const ChatPrivacyMaskerView({super.key});

  @override
  State<ChatPrivacyMaskerView> createState() => _ChatPrivacyMaskerViewState();
}

class _ChatPrivacyMaskerViewState extends State<ChatPrivacyMaskerView> {
  static const MethodChannel _mediaChannel =
      MethodChannel('com.novatoolbox/native_media');

  Uint8List? _sourceBytes;
  ui.Image? _decodedImage;

  final List<MaskItem> _masks = <MaskItem>[];
  final List<MaskItem> _redoStack = <MaskItem>[];

  MaskStyle _selectedStyle = MaskStyle.blackBar;
  Offset? _dragStartNormalized;
  Offset? _dragCurrentNormalized;
  bool _isExporting = false;

  @override
  void dispose() {
    _sourceBytes = null;
    _decodedImage?.dispose();
    _masks.clear();
    _redoStack.clear();
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

  Future<void> _pickImage() async {
    try {
      final Uint8List? bytes =
          await _mediaChannel.invokeMethod<Uint8List>('pickImage');
      if (bytes != null && mounted) {
        final Completer<ui.Image> completer = Completer<ui.Image>();
        ui.decodeImageFromList(bytes, (ui.Image img) => completer.complete(img));
        final ui.Image decoded = await completer.future;

        setState(() {
          _sourceBytes = bytes;
          _decodedImage?.dispose();
          _decodedImage = decoded;
          _masks.clear();
          _redoStack.clear();
          _dragStartNormalized = null;
          _dragCurrentNormalized = null;
        });
        _showToast('图片载入成功，拖拽即可打码');
      }
    } on Object catch (e) {
      _showToast('选择图片失败: $e');
    }
  }

  void _undo() {
    if (_masks.isEmpty) return;
    setState(() {
      final MaskItem popped = _masks.removeLast();
      _redoStack.add(popped);
    });
  }

  void _redo() {
    if (_redoStack.isEmpty) return;
    setState(() {
      final MaskItem item = _redoStack.removeLast();
      _masks.add(item);
    });
  }

  void _clearMasks() {
    if (_masks.isEmpty) return;
    setState(() {
      _masks.clear();
      _redoStack.clear();
    });
    _showToast('已清空全部遮罩');
  }

  // 纯内存高保真原图合成导出
  Future<void> _exportMaskedImage() async {
    if (_decodedImage == null) return;
    setState(() => _isExporting = true);

    try {
      final ui.Image src = _decodedImage!;
      final int imgW = src.width;
      final int imgH = src.height;

      final ui.PictureRecorder recorder = ui.PictureRecorder();
      final ui.Canvas canvas = ui.Canvas(recorder);

      // 1. 绘制底层原图
      canvas.drawImage(src, Offset.zero, Paint());

      // 2. 逐层高保真映射遮罩
      for (final MaskItem mask in _masks) {
        final Rect pixelRect = Rect.fromLTRB(
          mask.normalizedRect.left * imgW,
          mask.normalizedRect.top * imgH,
          mask.normalizedRect.right * imgW,
          mask.normalizedRect.bottom * imgH,
        );

        final Paint paint = Paint()
          ..color = mask.style.color
          ..style = PaintingStyle.fill
          ..isAntiAlias = true;

        if (mask.style.isCircle) {
          canvas.drawOval(pixelRect, paint);
        } else {
          final double radius = math.min(pixelRect.height, pixelRect.width) * 0.25;
          canvas.drawRRect(
            RRect.fromRectAndRadius(pixelRect, Radius.circular(radius)),
            paint,
          );
        }
      }

      final ui.Picture picture = recorder.endRecording();
      final ui.Image finalImg = await picture.toImage(imgW, imgH);
      final ByteData? byteData =
          await finalImg.toByteData(format: ui.ImageByteFormat.png);

      if (byteData != null && mounted) {
        final Uint8List outBytes = byteData.buffer.asUint8List();
        setState(() => _isExporting = false);
        _showExportSuccessDialog(outBytes);
      }
    } on Object catch (e) {
      if (mounted) {
        setState(() => _isExporting = false);
        _showToast('导出异常: $e');
      }
    }
  }

  void _showExportSuccessDialog(Uint8List bytes) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (BuildContext ctx) {
        final double kb = bytes.lengthInBytes / 1024;
        return Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Row(
                children: <Widget>[
                  const Icon(Icons.check_circle_rounded, color: Colors.green, size: 22),
                  const SizedBox(width: 8),
                  const Text('脱敏合成完成',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                  const Spacer(),
                  Chip(
                    visualDensity: VisualDensity.compact,
                    label: Text('${kb.toStringAsFixed(1)} KB (RAM)'),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Container(
                constraints: const BoxConstraints(maxHeight: 280),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Theme.of(ctx).colorScheme.outline),
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: Image.memory(bytes, fit: BoxFit.contain),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: <Widget>[
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => Navigator.pop(ctx),
                      icon: const Icon(Icons.close_rounded, size: 18),
                      label: const Text('关闭预览'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: () async {
                        Navigator.pop(ctx);
                        _showToast('脱敏图片已就绪 (内存纯净沙箱)');
                      },
                      icon: const Icon(Icons.done_all_rounded, size: 18),
                      label: const Text('完成'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('截图隐私遮罩'),
        actions: <Widget>[
          if (_decodedImage != null) ...<Widget>[
            IconButton(
              icon: const Icon(Icons.undo_rounded),
              tooltip: '撤销',
              onPressed: _masks.isNotEmpty ? _undo : null,
            ),
            IconButton(
              icon: const Icon(Icons.redo_rounded),
              tooltip: '重做',
              onPressed: _redoStack.isNotEmpty ? _redo : null,
            ),
            IconButton(
              icon: const Icon(Icons.clear_all_rounded),
              tooltip: '清空遮罩',
              onPressed: _masks.isNotEmpty ? _clearMasks : null,
            ),
            IconButton(
              icon: const Icon(Icons.download_done_rounded),
              tooltip: '合成导出',
              onPressed: _isExporting ? null : _exportMaskedImage,
            ),
          ],
          const SizedBox(width: 4),
        ],
      ),
      body: _decodedImage == null
          ? _buildEmptyView(theme)
          : _buildMaskWorkspace(theme),
    );
  }

  Widget _buildEmptyView(ThemeData theme) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Icon(Icons.shield_outlined, size: 64, color: theme.colorScheme.outline),
          const SizedBox(height: 16),
          const Text('导入聊天/订单截图进行隐私脱敏',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 6),
          Text('极简圆角几何遮罩 · 纯内存高保真处理',
              style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 24),
          FilledButton.tonalIcon(
            onPressed: _pickImage,
            icon: const Icon(Icons.add_photo_alternate_outlined),
            label: const Text('选取图片'),
          ),
        ],
      ),
    );
  }

  Widget _buildMaskWorkspace(ThemeData theme) {
    return Column(
      children: <Widget>[
        // 样式切换工具栏
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            color: theme.colorScheme.surface,
            border: Border(bottom: BorderSide(color: theme.colorScheme.outline)),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: MaskStyle.values.map((MaskStyle style) {
                      final bool isSelected = _selectedStyle == style;
                      return Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: ChoiceChip(
                          avatar: Container(
                            width: 14,
                            height: 14,
                            decoration: BoxDecoration(
                              color: style.color,
                              shape: style.isCircle ? BoxShape.circle : BoxShape.rectangle,
                              borderRadius: style.isCircle ? null : BorderRadius.circular(2),
                              border: Border.all(color: Colors.grey.withValues(alpha: 0.5)),
                            ),
                          ),
                          label: Text(style.label),
                          selected: isSelected,
                          onSelected: (bool sel) {
                            if (sel) setState(() => _selectedStyle = style);
                          },
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              TextButton.icon(
                onPressed: _pickImage,
                icon: const Icon(Icons.refresh_rounded, size: 16),
                label: const Text('换图'),
              ),
            ],
          ),
        ),

        // 交互式打码画布
        Expanded(
          child: Container(
            color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
            padding: const EdgeInsets.all(12),
            child: LayoutBuilder(
              builder: (BuildContext context, BoxConstraints constraints) {
                return Center(
                  child: MaskCanvasViewer(
                    image: _decodedImage!,
                    viewportSize: Size(constraints.maxWidth, constraints.maxHeight),
                    masks: _masks,
                    currentStyle: _selectedStyle,
                    dragStart: _dragStartNormalized,
                    dragCurrent: _dragCurrentNormalized,
                    onDragStart: (Offset normalized) {
                      setState(() {
                        _dragStartNormalized = normalized;
                        _dragCurrentNormalized = normalized;
                      });
                    },
                    onDragUpdate: (Offset normalized) {
                      setState(() {
                        _dragCurrentNormalized = normalized;
                      });
                    },
                    onDragEnd: (Rect? normalizedRect) {
                      if (normalizedRect != null &&
                          normalizedRect.width > 0.005 &&
                          normalizedRect.height > 0.005) {
                        setState(() {
                          _masks.add(MaskItem(
                              normalizedRect: normalizedRect, style: _selectedStyle));
                          _redoStack.clear();
                        });
                      }
                      setState(() {
                        _dragStartNormalized = null;
                        _dragCurrentNormalized = null;
                      });
                    },
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}

// ---------------- 几何归一化手势画布 ----------------
class MaskCanvasViewer extends StatelessWidget {
  final ui.Image image;
  final Size viewportSize;
  final List<MaskItem> masks;
  final MaskStyle currentStyle;
  final Offset? dragStart;
  final Offset? dragCurrent;
  final ValueChanged<Offset> onDragStart;
  final ValueChanged<Offset> onDragUpdate;
  final ValueChanged<Rect?> onDragEnd;

  const MaskCanvasViewer({
    super.key,
    required this.image,
    required this.viewportSize,
    required this.masks,
    required this.currentStyle,
    required this.dragStart,
    required this.dragCurrent,
    required this.onDragStart,
    required this.onDragUpdate,
    required this.onDragEnd,
  });

  Rect _calculateImageDestRect(Size containerSize) {
    final Size srcSize = Size(image.width.toDouble(), image.height.toDouble());
    final FittedSizes fitted =
        applyBoxFit(BoxFit.contain, srcSize, containerSize);
    final double left =
        (containerSize.width - fitted.destination.width) / 2.0;
    final double top =
        (containerSize.height - fitted.destination.height) / 2.0;
    return Rect.fromLTWH(
        left, top, fitted.destination.width, fitted.destination.height);
  }

  Offset _normalizeOffset(Offset localPos, Rect destRect) {
    final double x =
        ((localPos.dx - destRect.left) / destRect.width).clamp(0.0, 1.0);
    final double y =
        ((localPos.dy - destRect.top) / destRect.height).clamp(0.0, 1.0);
    return Offset(x, y);
  }

  @override
  Widget build(BuildContext context) {
    final Rect destRect = _calculateImageDestRect(viewportSize);

    return GestureDetector(
      onPanStart: (DragStartDetails details) {
        final Offset normalized = _normalizeOffset(details.localPosition, destRect);
        onDragStart(normalized);
      },
      onPanUpdate: (DragUpdateDetails details) {
        final Offset normalized = _normalizeOffset(details.localPosition, destRect);
        onDragUpdate(normalized);
      },
      onPanEnd: (DragEndDetails details) {
        if (dragStart != null && dragCurrent != null) {
          final Rect rect = Rect.fromPoints(dragStart!, dragCurrent!);
          onDragEnd(rect);
        } else {
          onDragEnd(null);
        }
      },
      child: CustomPaint(
        size: Size(destRect.width, destRect.height),
        painter: MaskPainter(
          image: image,
          masks: masks,
          currentStyle: currentStyle,
          dragStart: dragStart,
          dragCurrent: dragCurrent,
        ),
      ),
    );
  }
}

class MaskPainter extends CustomPainter {
  final ui.Image image;
  final List<MaskItem> masks;
  final MaskStyle currentStyle;
  final Offset? dragStart;
  final Offset? dragCurrent;

  MaskPainter({
    required this.image,
    required this.masks,
    required this.currentStyle,
    required this.dragStart,
    required this.dragCurrent,
  });

  @override
  void paint(Canvas canvas, Size size) {
    // 绘制底图
    final Rect srcRect =
        Rect.fromLTWH(0, 0, image.width.toDouble(), image.height.toDouble());
    final Rect dstRect = Rect.fromLTWH(0, 0, size.width, size.height);
    canvas.drawImageRect(image, srcRect, dstRect, Paint());

    // 绘制已保存遮罩
    for (final MaskItem item in masks) {
      final Rect r = Rect.fromLTRB(
        item.normalizedRect.left * size.width,
        item.normalizedRect.top * size.height,
        item.normalizedRect.right * size.width,
        item.normalizedRect.bottom * size.height,
      );

      final Paint paint = Paint()
        ..color = item.style.color
        ..style = PaintingStyle.fill
        ..isAntiAlias = true;

      if (item.style.isCircle) {
        canvas.drawOval(r, paint);
      } else {
        final double radius = math.min(r.height, r.width) * 0.25;
        canvas.drawRRect(RRect.fromRectAndRadius(r, Radius.circular(radius)), paint);
      }
    }

    // 绘制实时拖拽预览
    if (dragStart != null && dragCurrent != null) {
      final Rect activeRect = Rect.fromPoints(
        Offset(dragStart!.dx * size.width, dragStart!.dy * size.height),
        Offset(dragCurrent!.dx * size.width, dragCurrent!.dy * size.height),
      );

      final Paint activePaint = Paint()
        ..color = currentStyle.color.withValues(alpha: 0.85)
        ..style = PaintingStyle.fill
        ..isAntiAlias = true;

      final Paint borderPaint = Paint()
        ..color = Colors.blueAccent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5;

      if (currentStyle.isCircle) {
        canvas.drawOval(activeRect, activePaint);
        canvas.drawOval(activeRect, borderPaint);
      } else {
        final double radius = math.min(activeRect.height, activeRect.width) * 0.25;
        final RRect rrect =
            RRect.fromRectAndRadius(activeRect, Radius.circular(radius));
        canvas.drawRRect(rrect, activePaint);
        canvas.drawRRect(rrect, borderPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant MaskPainter oldDelegate) => true;
}
