import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/plugin/base_tool_plugin.dart';

/// 运动形式
enum MotionMode {
  staticSign('静止标牌', '全屏矢量自适应 · 居中定格展示'),
  marquee('平滑滚动', '匀速跑马灯 · 适合超长文案与远距动效');

  final String label;
  final String desc;
  const MotionMode(this.label, this.desc);
}

/// 渲染风格引擎
enum DisplayStyle {
  swissBold('瑞士排版', '大字紧凑负字距 · 极简冷淡'),
  dotMatrix('工业点阵', '精密 LED 发光矩阵 · 复古硬件'),
  pureMono('单色纯粹', '等宽极简 · 无任何多余修饰');

  final String label;
  final String desc;
  const DisplayStyle(this.label, this.desc);
}

/// 专业调色方案
class PaletteTone {
  final String name;
  final Color fgColor;
  final Color bgColor;
  final Color accentColor;

  const PaletteTone({
    required this.name,
    required this.fgColor,
    required this.bgColor,
    required this.accentColor,
  });
}

const List<PaletteTone> kStudioPalettes = <PaletteTone>[
  PaletteTone(
    name: '曜石极黑',
    fgColor: Color(0xFFF4F4F5),
    bgColor: Color(0xFF09090B),
    accentColor: Color(0xFF71717A),
  ),
  PaletteTone(
    name: '琥珀金标',
    fgColor: Color(0xFFF59E0B),
    bgColor: Color(0xFF0C0A09),
    accentColor: Color(0xFFD97706),
  ),
  PaletteTone(
    name: '极光冷绿',
    fgColor: Color(0xFF10B981),
    bgColor: Color(0xFF022C22),
    accentColor: Color(0xFF059669),
  ),
  PaletteTone(
    name: '钛银高反',
    fgColor: Color(0xFF09090B),
    bgColor: Color(0xFFE4E4E7),
    accentColor: Color(0xFFA1A1AA),
  ),
  PaletteTone(
    name: '深空蓝调',
    fgColor: Color(0xFF38BDF8),
    bgColor: Color(0xFF082F49),
    accentColor: Color(0xFF0284C7),
  ),
];

class FullscreenBarragePlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'fullscreen_barrage',
        version: '2.1.0',
        name: '全屏视显标牌',
        description: '瑞士国际排版与工业点阵引擎，支持全屏静止自适应标牌与平滑跑马灯',
        category: '常用与效率',
        icon: Icons.fullscreen_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const BarrageStudioView();
  }

  @override
  void dispose() {}
}

class BarrageStudioView extends StatefulWidget {
  const BarrageStudioView({super.key});

  @override
  State<BarrageStudioView> createState() => _BarrageStudioViewState();
}

class _BarrageStudioViewState extends State<BarrageStudioView> {
  late final TextEditingController _textController;

  MotionMode _selectedMotion = MotionMode.staticSign; // 默认静止标牌
  PaletteTone _selectedPalette = kStudioPalettes[0];
  DisplayStyle _selectedStyle = DisplayStyle.swissBold;
  double _speed = 1.0;
  double _scale = 1.0;

  @override
  void initState() {
    super.initState();
    _textController = TextEditingController(text: '接机 · 张三先生');
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  void _launchFullscreen() {
    final String text = _textController.text.trim();
    if (text.isEmpty) return;

    Navigator.of(context).push(
      PageRouteBuilder<void>(
        pageBuilder: (BuildContext context, Animation<double> a1, Animation<double> a2) =>
            FullscreenDisplayStage(
          text: text,
          motion: _selectedMotion,
          palette: _selectedPalette,
          style: _selectedStyle,
          speed: _speed,
          scale: _scale,
        ),
        transitionsBuilder: (BuildContext context, Animation<double> a1, Animation<double> a2, Widget child) {
          return FadeTransition(opacity: a1, child: child);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    const Color surfaceDark = Color(0xFF18181B);
    const Color borderDark = Color(0xFF27272A);

    return Scaffold(
      backgroundColor: const Color(0xFF09090B),
      appBar: AppBar(
        backgroundColor: const Color(0xFF09090B),
        elevation: 0,
        title: const Text(
          'PRECISION DISPLAY',
          style: TextStyle(
            color: Color(0xFFF4F4F5),
            fontSize: 14,
            letterSpacing: 2.0,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        children: <Widget>[
          // 顶部实时监视器
          Container(
            height: 130,
            decoration: BoxDecoration(
              color: _selectedPalette.bgColor,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: borderDark, width: 1.5),
              boxShadow: <BoxShadow>[
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.6),
                  blurRadius: 16,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: Stack(
                fit: StackFit.expand,
                children: <Widget>[
                  CustomPaint(
                    painter: StudioGridPainter(
                        lineColor: _selectedPalette.fgColor.withValues(alpha: 0.05)),
                  ),
                  Center(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          _textController.text.isEmpty ? 'PREVIEW' : _textController.text,
                          style: TextStyle(
                            color: _selectedPalette.fgColor,
                            fontSize: 32 * _scale,
                            fontWeight: FontWeight.w900,
                            letterSpacing: _selectedStyle == DisplayStyle.swissBold
                                ? -1.0
                                : 2.0,
                            fontFamily: _selectedStyle == DisplayStyle.dotMatrix
                                ? 'monospace'
                                : null,
                          ),
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    top: 10,
                    left: 12,
                    child: Text(
                      '${_selectedMotion.label.toUpperCase()} · ${_selectedStyle.label}',
                      style: TextStyle(
                        fontSize: 9,
                        letterSpacing: 1.2,
                        fontWeight: FontWeight.w600,
                        color: _selectedPalette.fgColor.withValues(alpha: 0.4),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // 核心：模式选择切换 (静止 / 滚动)
          Text(
            'MOTION MODE / 呈现模式',
            style: TextStyle(
                fontSize: 11,
                letterSpacing: 1.5,
                fontWeight: FontWeight.w600,
                color: const Color(0xFFA1A1AA)),
          ),
          const SizedBox(height: 8),
          Row(
            children: MotionMode.values.map((MotionMode mode) {
              final bool isSelected = _selectedMotion == mode;
              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: InkWell(
                    onTap: () => setState(() => _selectedMotion = mode),
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
                      decoration: BoxDecoration(
                        color: isSelected ? const Color(0xFF27272A) : surfaceDark,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected
                              ? const Color(0xFFF4F4F5)
                              : borderDark,
                          width: isSelected ? 1.5 : 1.0,
                        ),
                      ),
                      child: Column(
                        children: <Widget>[
                          Text(
                            mode.label,
                            style: TextStyle(
                              color: isSelected
                                  ? const Color(0xFFF4F4F5)
                                  : const Color(0xFFA1A1AA),
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            mode == MotionMode.staticSign
                                ? '大字定格 · 自适应'
                                : '横向跑马灯 · 调速',
                            style: TextStyle(
                              color: isSelected
                                  ? const Color(0xFFD4D4D8)
                                  : const Color(0xFF71717A),
                              fontSize: 10,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // 文本输入
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            decoration: BoxDecoration(
              color: surfaceDark,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: borderDark),
            ),
            child: TextField(
              controller: _textController,
              onChanged: (_) => setState(() {}),
              style: const TextStyle(
                color: Color(0xFFF4F4F5),
                fontSize: 16,
                fontWeight: FontWeight.w600,
                letterSpacing: 0.5,
              ),
              decoration: InputDecoration(
                hintText: '输入标牌内容...',
                hintStyle: TextStyle(
                    color: const Color(0xFF71717A).withValues(alpha: 0.7),
                    fontSize: 14),
                border: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 20),

          // 调色方案
          Text(
            'COLORWAYS / 色彩方案',
            style: TextStyle(
                fontSize: 11,
                letterSpacing: 1.5,
                fontWeight: FontWeight.w600,
                color: const Color(0xFFA1A1AA)),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: kStudioPalettes.map((PaletteTone tone) {
                final bool isSelected = _selectedPalette.name == tone.name;
                return Padding(
                  padding: const EdgeInsets.only(right: 10),
                  child: InkWell(
                    onTap: () => setState(() => _selectedPalette = tone),
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        color: surfaceDark,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected ? tone.fgColor : borderDark,
                          width: isSelected ? 1.5 : 1.0,
                        ),
                      ),
                      child: Row(
                        children: <Widget>[
                          Container(
                            width: 12,
                            height: 12,
                            decoration: BoxDecoration(
                              color: tone.fgColor,
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.black26),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            tone.name,
                            style: TextStyle(
                              color: isSelected
                                  ? const Color(0xFFF4F4F5)
                                  : const Color(0xFFA1A1AA),
                              fontSize: 12,
                              fontWeight: isSelected
                                  ? FontWeight.w700
                                  : FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 20),

          // 排版风格
          Text(
            'TYPOGRAPHY / 字体引擎',
            style: TextStyle(
                fontSize: 11,
                letterSpacing: 1.5,
                fontWeight: FontWeight.w600,
                color: const Color(0xFFA1A1AA)),
          ),
          const SizedBox(height: 8),
          Row(
            children: DisplayStyle.values.map((DisplayStyle style) {
              final bool isSelected = _selectedStyle == style;
              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 3),
                  child: InkWell(
                    onTap: () => setState(() => _selectedStyle = style),
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 10),
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: isSelected ? const Color(0xFF27272A) : surfaceDark,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected
                              ? const Color(0xFFF4F4F5)
                              : borderDark,
                          width: isSelected ? 1.5 : 1.0,
                        ),
                      ),
                      child: Text(
                        style.label,
                        style: TextStyle(
                          color: isSelected
                              ? const Color(0xFFF4F4F5)
                              : const Color(0xFFA1A1AA),
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // 参数控制
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: surfaceDark,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: borderDark),
            ),
            child: Column(
              children: <Widget>[
                if (_selectedMotion == MotionMode.marquee) ...<Widget>[
                  Row(
                    children: <Widget>[
                      const Text('滚动速度 / SPEED',
                          style: TextStyle(
                              fontSize: 11,
                              letterSpacing: 1.0,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFFA1A1AA))),
                      const Spacer(),
                      Text('${_speed.toStringAsFixed(1)}x',
                          style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFFF4F4F5))),
                    ],
                  ),
                  SliderTheme(
                    data: SliderTheme.of(context).copyWith(
                      activeTrackColor: const Color(0xFFF4F4F5),
                      inactiveTrackColor: borderDark,
                      thumbColor: const Color(0xFFF4F4F5),
                      trackHeight: 2.0,
                      thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                    ),
                    child: Slider(
                      value: _speed,
                      min: 0.4,
                      max: 2.5,
                      divisions: 10,
                      onChanged: (double v) => setState(() => _speed = v),
                    ),
                  ),
                  const SizedBox(height: 8),
                ],
                Row(
                  children: <Widget>[
                    const Text('字阶比例 / SCALE',
                        style: TextStyle(
                            fontSize: 11,
                            letterSpacing: 1.0,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFFA1A1AA))),
                    const Spacer(),
                    Text('${(_scale * 100).toInt()}%',
                        style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFF4F4F5))),
                  ],
                ),
                SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    activeTrackColor: const Color(0xFFF4F4F5),
                    inactiveTrackColor: borderDark,
                    thumbColor: const Color(0xFFF4F4F5),
                    trackHeight: 2.0,
                    thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                  ),
                  child: Slider(
                    value: _scale,
                    min: 0.6,
                    max: 1.6,
                    divisions: 10,
                    onChanged: (double v) => setState(() => _scale = v),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),

          // 进入全屏按钮
          SizedBox(
            height: 52,
            child: FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFF4F4F5),
                foregroundColor: const Color(0xFF09090B),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: _launchFullscreen,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  const Icon(Icons.crop_free_rounded, size: 18),
                  const SizedBox(width: 8),
                  Text(
                    _selectedMotion == MotionMode.staticSign
                        ? 'LAUNCH STATIC SIGN / 全屏静止标牌'
                        : 'LAUNCH MARQUEE / 全屏滚动跑马灯',
                    style: const TextStyle(
                        fontSize: 13, letterSpacing: 1.5, fontWeight: FontWeight.w800),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

// ---------------- 全屏沉浸式渲染视窗 ----------------
class FullscreenDisplayStage extends StatefulWidget {
  final String text;
  final MotionMode motion;
  final PaletteTone palette;
  final DisplayStyle style;
  final double speed;
  final double scale;

  const FullscreenDisplayStage({
    super.key,
    required this.text,
    required this.motion,
    required this.palette,
    required this.style,
    required this.speed,
    required this.scale,
  });

  @override
  State<FullscreenDisplayStage> createState() => _FullscreenDisplayStageState();
}

class _FullscreenDisplayStageState extends State<FullscreenDisplayStage>
    with SingleTickerProviderStateMixin {
  late final AnimationController _motionController;
  bool _isFreeze = false;

  @override
  void initState() {
    super.initState();
    // 强制锁横屏 + 沉浸式隐藏状态栏/导航栏
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    SystemChrome.setPreferredOrientations(<DeviceOrientation>[
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    final int durationMs = (7000 / widget.speed).toInt();
    _motionController = AnimationController(
      vsync: this,
      duration: Duration(milliseconds: durationMs),
    );

    if (widget.motion == MotionMode.marquee) {
      _motionController.repeat();
    }
  }

  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations(DeviceOrientation.values);
    _motionController.dispose();
    super.dispose();
  }

  void _toggleFreeze() {
    if (widget.motion == MotionMode.staticSign) return;
    setState(() {
      _isFreeze = !_isFreeze;
      if (_isFreeze) {
        _motionController.stop();
      } else {
        _motionController.repeat();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: widget.palette.bgColor,
      body: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: _toggleFreeze, // 单击瞬时定格/恢复
        onDoubleTap: () => Navigator.of(context).pop(), // 双击退出
        child: Stack(
          fit: StackFit.expand,
          children: <Widget>[
            // 工业点阵背景
            if (widget.style == DisplayStyle.dotMatrix)
              CustomPaint(
                painter: MatrixLedGridPainter(
                  dotColor: widget.palette.fgColor.withValues(alpha: 0.08),
                  dotSpacing: 14.0,
                ),
              ),

            // 主视显内容区
            if (widget.motion == MotionMode.staticSign)
              _buildStaticSignView()
            else
              _buildMarqueeView(),
          ],
        ),
      ),
    );
  }

  // 1. 静止标牌：矢量自动缩放 (Auto-Fit)，绝不截断
  Widget _buildStaticSignView() {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final double maxAllowedHeight = constraints.maxHeight * 0.78 * widget.scale;

        return Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 40),
            child: FittedBox(
              fit: BoxFit.contain,
              child: Text(
                widget.text,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: widget.palette.fgColor,
                  fontSize: maxAllowedHeight,
                  fontWeight: FontWeight.w900,
                  letterSpacing:
                      widget.style == DisplayStyle.swissBold ? -2.0 : 4.0,
                  fontFamily: widget.style == DisplayStyle.dotMatrix
                      ? 'monospace'
                      : null,
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  // 2. 滚动跑马灯
  Widget _buildMarqueeView() {
    return AnimatedBuilder(
      animation: _motionController,
      builder: (BuildContext context, Widget? child) {
        return LayoutBuilder(
          builder: (BuildContext context, BoxConstraints constraints) {
            final double baseFontSize = (constraints.maxHeight * 0.58) * widget.scale;

            final TextStyle textStyle = TextStyle(
              color: widget.palette.fgColor,
              fontSize: baseFontSize,
              fontWeight: FontWeight.w900,
              letterSpacing: widget.style == DisplayStyle.swissBold ? -2.0 : 4.0,
              fontFamily: widget.style == DisplayStyle.dotMatrix
                  ? 'monospace'
                  : null,
            );

            final TextPainter painter = TextPainter(
              text: TextSpan(text: widget.text, style: textStyle),
              textDirection: TextDirection.ltr,
            )..layout();

            final double textWidth = painter.width;
            final double totalDistance = constraints.maxWidth + textWidth;
            final double progress = _motionController.value;

            final double offsetX = constraints.maxWidth - (progress * totalDistance);

            return Transform.translate(
              offset: Offset(offsetX, (constraints.maxHeight - painter.height) / 2),
              child: Text(widget.text,
                  maxLines: 1, softWrap: false, style: textStyle),
            );
          },
        );
      },
    );
  }
}

// ---------------- Canvas 视觉着色器 ----------------
class StudioGridPainter extends CustomPainter {
  final Color lineColor;
  StudioGridPainter({required this.lineColor});

  @override
  void paint(Canvas canvas, Size size) {
    final Paint paint = Paint()
      ..color = lineColor
      ..strokeWidth = 1.0;

    const double step = 20.0;
    for (double x = 0; x < size.width; x += step) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += step) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant StudioGridPainter oldDelegate) => false;
}

class MatrixLedGridPainter extends CustomPainter {
  final Color dotColor;
  final double dotSpacing;

  MatrixLedGridPainter({required this.dotColor, required this.dotSpacing});

  @override
  void paint(Canvas canvas, Size size) {
    final Paint paint = Paint()
      ..color = dotColor
      ..style = PaintingStyle.fill;

    for (double x = dotSpacing / 2; x < size.width; x += dotSpacing) {
      for (double y = dotSpacing / 2; y < size.height; y += dotSpacing) {
        canvas.drawCircle(Offset(x, y), 1.5, paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant MatrixLedGridPainter oldDelegate) => false;
}
