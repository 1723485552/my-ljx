import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/plugin/base_tool_plugin.dart';

enum ReverseMode {
  character('字符倒序', 'abcdef -> fedcba'),
  shuffleChar('字符打乱', '随机重排每个字符'),
  words('单词倒序', 'hello world -> world hello'),
  shuffleWords('词语打乱', '随机重排词组顺序'),
  lines('按行倒序', '倒置段落行首尾'),
  invertCase('大小写翻转', 'AbC -> aBc');

  const ReverseMode(this.label, this.hint);

  final String label;
  final String hint;
}

class TextReverserPlugin extends BaseToolPlugin {
  TextReverserPlugin();

  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'text_reverser',
        version: '1.1.0',
        name: '文本反转与打乱',
        description: '支持字符倒序、随机打乱乱序、单词/行翻转及大小写反转',
        category: '文本与效率',
        icon: Icons.shuffle_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const TextReverserView();
  }

  @override
  void dispose() {}
}

class TextReverserView extends StatefulWidget {
  const TextReverserView({super.key});

  @override
  State<TextReverserView> createState() => _TextReverserViewState();
}

class _TextReverserViewState extends State<TextReverserView> {
  late final TextEditingController _inputController;
  ReverseMode _selectedMode = ReverseMode.character;
  String _outputResult = '';
  final Random _random = Random();

  @override
  void initState() {
    super.initState();
    _inputController = TextEditingController();
    _inputController.addListener(_handleInputChanged);
  }

  void _handleInputChanged() {
    _processText();
  }

  void _processText() {
    if (!mounted) return;

    final String text = _inputController.text;
    if (text.isEmpty) {
      setState(() => _outputResult = '');
      return;
    }

    String result = '';
    switch (_selectedMode) {
      case ReverseMode.character:
        result = text.split('').reversed.join('');
        break;

      case ReverseMode.shuffleChar:
        final List<String> chars = text.split('');
        chars.shuffle(_random);
        result = chars.join('');
        break;

      case ReverseMode.words:
        result = text.split(RegExp(r'(\s+)')).reversed.join(' ');
        break;

      case ReverseMode.shuffleWords:
        final List<String> words =
            text.split(RegExp(r'\s+')).where((String s) => s.isNotEmpty).toList();
        words.shuffle(_random);
        result = words.join(' ');
        break;

      case ReverseMode.lines:
        result = text.split('\n').reversed.join('\n');
        break;

      case ReverseMode.invertCase:
        final StringBuffer buffer = StringBuffer();
        for (final int rune in text.runes) {
          final String char = String.fromCharCode(rune);
          if (char == char.toUpperCase()) {
            buffer.write(char.toLowerCase());
          } else {
            buffer.write(char.toUpperCase());
          }
        }
        result = buffer.toString();
        break;
    }

    setState(() => _outputResult = result);
  }

  Future<void> _copyResult() async {
    if (_outputResult.isEmpty) return;
    await Clipboard.setData(ClipboardData(text: _outputResult));
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('已复制转换结果'),
          duration: Duration(milliseconds: 1000),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  void dispose() {
    _inputController.removeListener(_handleInputChanged);
    _inputController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final bool isRandomMode =
        _selectedMode == ReverseMode.shuffleChar || _selectedMode == ReverseMode.shuffleWords;

    return Scaffold(
      appBar: AppBar(
        title: const Text('文本反转与打乱'),
        actions: <Widget>[
          if (isRandomMode)
            IconButton(
              icon: const Icon(Icons.refresh_rounded),
              tooltip: '重新随机打乱',
              onPressed: _outputResult.isNotEmpty ? _processText : null,
            ),
          IconButton(
            icon: const Icon(Icons.copy_rounded, size: 20),
            tooltip: '复制结果',
            onPressed: _outputResult.isNotEmpty ? _copyResult : null,
          ),
          IconButton(
            icon: const Icon(Icons.clear_rounded, size: 22),
            tooltip: '清空输入',
            onPressed: _inputController.text.isNotEmpty
                ? () {
                    _inputController.clear();
                  }
                : null,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          // 模式选择流式布局
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: ReverseMode.values.map((ReverseMode mode) {
              final bool isSelected = _selectedMode == mode;
              return ChoiceChip(
                label: Text(mode.label),
                selected: isSelected,
                onSelected: (bool selected) {
                  if (selected && mounted) {
                    setState(() => _selectedMode = mode);
                    _processText();
                  }
                },
              );
            }).toList(),
          ),
          const SizedBox(height: 16),

          // 输入卡片
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: theme.colorScheme.outline),
            ),
            child: TextField(
              controller: _inputController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: '输入需要处理的文本 (${_selectedMode.hint})...',
                hintStyle: TextStyle(
                  color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.6),
                  fontSize: 14,
                ),
                border: InputBorder.none,
                isDense: true,
                contentPadding: EdgeInsets.zero,
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 输出结果卡片
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: theme.colorScheme.primary.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: theme.colorScheme.primary.withValues(alpha: 0.2)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Text(
                      '处理结果',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: theme.colorScheme.primary,
                      ),
                    ),
                    const Spacer(),
                    if (_outputResult.isNotEmpty)
                      Text(
                        '${_outputResult.length} 字符',
                        style: TextStyle(
                          fontSize: 11,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 10),
                SelectableText(
                  _outputResult.isEmpty ? '等待输入中...' : _outputResult,
                  style: TextStyle(
                    fontSize: 15,
                    height: 1.5,
                    fontFamily: 'monospace',
                    color: _outputResult.isEmpty
                        ? theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.5)
                        : theme.colorScheme.onSurface,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
