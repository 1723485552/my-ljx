import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import '../../../core/plugin/base_tool_plugin.dart';

class LanguageItem {
  final String code;
  final String name;
  final String nativeName;

  const LanguageItem({
    required this.code,
    required this.name,
    required this.nativeName,
  });
}

const List<LanguageItem> kAllSupportedLanguages = <LanguageItem>[
  LanguageItem(code: 'auto', name: '自动检测', nativeName: 'Auto Detect'),
  LanguageItem(code: 'zh-CN', name: '中文 (简体)', nativeName: '简体中文'),
  LanguageItem(code: 'zh-TW', name: '中文 (繁体)', nativeName: '繁體中文'),
  LanguageItem(code: 'en', name: '英语', nativeName: 'English'),
  LanguageItem(code: 'ja', name: '日语', nativeName: '日本語'),
  LanguageItem(code: 'ko', name: '韩语', nativeName: '한국어'),
  LanguageItem(code: 'fr', name: '法语', nativeName: 'Français'),
  LanguageItem(code: 'de', name: '德语', nativeName: 'Deutsch'),
  LanguageItem(code: 'es', name: '西班牙语', nativeName: 'Español'),
  LanguageItem(code: 'ru', name: '俄语', nativeName: 'Русский'),
  LanguageItem(code: 'it', name: '意大利语', nativeName: 'Italiano'),
  LanguageItem(code: 'pt', name: '葡萄牙语', nativeName: 'Português'),
  LanguageItem(code: 'th', name: '泰语', nativeName: 'ภาษาไทย'),
  LanguageItem(code: 'vi', name: '越南语', nativeName: 'Tiếng Việt'),
  LanguageItem(code: 'id', name: '印尼语', nativeName: 'Bahasa Indonesia'),
  LanguageItem(code: 'ar', name: '阿拉伯语', nativeName: 'العربية'),
  LanguageItem(code: 'hi', name: '印地语', nativeName: 'हिन्दी'),
  LanguageItem(code: 'nl', name: '荷兰语', nativeName: 'Nederlands'),
  LanguageItem(code: 'pl', name: '波兰语', nativeName: 'Polski'),
  LanguageItem(code: 'tr', name: '土耳其语', nativeName: 'Türkçe'),
  LanguageItem(code: 'el', name: '希腊语', nativeName: 'Ελληνικά'),
];

class InstantTranslatorPlugin extends BaseToolPlugin {
  @override
  ToolManifest get manifest => const ToolManifest(
        id: 'instant_translator',
        version: '1.2.0',
        name: '极简多语速翻',
        description: '全球 20+ 语种自由双向互译引擎，支持智能嗅探与自定义目标语种',
        category: '常用与效率',
        icon: Icons.translate_rounded,
      );

  @override
  Widget buildView(BuildContext context) {
    return const InstantTranslatorView();
  }

  @override
  void dispose() {}
}

class InstantTranslatorView extends StatefulWidget {
  const InstantTranslatorView({super.key});

  @override
  State<InstantTranslatorView> createState() => _InstantTranslatorViewState();
}

class _InstantTranslatorViewState extends State<InstantTranslatorView> {
  late final TextEditingController _inputController;

  String _sourceLang = 'auto';
  String _targetLang = 'en'; // 默认翻译为英文，支持用户任意自选
  String _translatedResult = '';
  String _detectedLangCode = '';
  bool _isLoading = false;
  Timer? _debounceTimer;

  // 快捷高频目标语言标签
  final List<String> _quickTargetLangs = const <String>[
    'zh-CN',
    'en',
    'ja',
    'ko',
    'de',
    'fr',
    'es',
    'ru',
  ];

  @override
  void initState() {
    super.initState();
    _inputController = TextEditingController();
    _inputController.addListener(_onInputChanged);
  }

  @override
  void dispose() {
    _debounceTimer?.cancel();
    _inputController.removeListener(_onInputChanged);
    _inputController.dispose();
    super.dispose();
  }

  LanguageItem _getLangByCode(String code) {
    return kAllSupportedLanguages.firstWhere(
      (LanguageItem l) => l.code == code,
      orElse: () => LanguageItem(code: code, name: code, nativeName: code),
    );
  }

  void _onInputChanged() {
    _debounceTimer?.cancel();
    final String text = _inputController.text.trim();
    if (text.isEmpty) {
      if (mounted) {
        setState(() {
          _translatedResult = '';
          _detectedLangCode = '';
          _isLoading = false;
        });
      }
      return;
    }

    _debounceTimer = Timer(const Duration(milliseconds: 400), () {
      _executeTranslation(text);
    });
  }

  Future<void> _executeTranslation(String text) async {
    if (!mounted || text.isEmpty) return;

    setState(() => _isLoading = true);

    try {
      final Uri uri = Uri.parse(
        'https://translate.googleapis.com/translate_a/single?client=gtx&sl=$_sourceLang&tl=$_targetLang&dt=t&q=${Uri.encodeComponent(text)}',
      );

      final http.Response response =
          await http.get(uri).timeout(const Duration(seconds: 8));

      if (response.statusCode == 200) {
        final dynamic rawJson = jsonDecode(response.body);
        if (rawJson is List && rawJson.isNotEmpty) {
          final StringBuffer resultBuffer = StringBuffer();
          final dynamic sentences = rawJson[0];
          if (sentences is List) {
            for (final dynamic item in sentences) {
              if (item is List && item.isNotEmpty && item[0] != null) {
                resultBuffer.write(item[0]);
              }
            }
          }

          String detected = '';
          if (rawJson.length > 2 && rawJson[2] is String) {
            detected = rawJson[2] as String;
          }

          if (mounted) {
            setState(() {
              _translatedResult = resultBuffer.toString();
              _detectedLangCode = detected;
              _isLoading = false;
            });
          }
          return;
        }
      }

      if (mounted) {
        setState(() {
          _translatedResult = '翻译响应异常，请重试';
          _isLoading = false;
        });
      }
    } on Object catch (_) {
      if (mounted) {
        setState(() {
          _translatedResult = '网络请求失败，请确保设备联网';
          _isLoading = false;
        });
      }
    }
  }

  void _swapLanguages() {
    String newSource = _targetLang;
    String newTarget = _sourceLang;

    if (_sourceLang == 'auto') {
      newTarget = _detectedLangCode.isNotEmpty ? _detectedLangCode : 'zh-CN';
    }

    setState(() {
      _sourceLang = newSource;
      _targetLang = newTarget;
    });

    if (_inputController.text.isNotEmpty &&
        _translatedResult.isNotEmpty &&
        !_translatedResult.startsWith('网络') &&
        !_translatedResult.startsWith('翻译')) {
      final String oldResult = _translatedResult;
      _inputController.text = oldResult;
    } else if (_inputController.text.isNotEmpty) {
      _executeTranslation(_inputController.text.trim());
    }
  }

  void _openLanguagePicker({required bool isSource}) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF18181B),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (BuildContext ctx) {
        return _LanguagePickerSheet(
          isSource: isSource,
          currentSelectedCode: isSource ? _sourceLang : _targetLang,
          onSelected: (String code) {
            Navigator.pop(ctx);
            setState(() {
              if (isSource) {
                _sourceLang = code;
              } else {
                _targetLang = code;
              }
            });
            if (_inputController.text.isNotEmpty) {
              _executeTranslation(_inputController.text.trim());
            }
          },
        );
      },
    );
  }

  void _copyResult() {
    if (_translatedResult.isEmpty) return;
    Clipboard.setData(ClipboardData(text: _translatedResult));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('已复制译文'),
        duration: Duration(milliseconds: 1000),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _pasteFromClipboard() async {
    final ClipboardData? data = await Clipboard.getData(Clipboard.kTextPlain);
    if (data != null && data.text != null && data.text!.isNotEmpty) {
      _inputController.text = data.text!;
    }
  }

  @override
  Widget build(BuildContext context) {
    const Color bgDark = Color(0xFF09090B);
    const Color surfaceDark = Color(0xFF18181B);
    const Color borderDark = Color(0xFF27272A);
    const Color textPrimary = Color(0xFFF4F4F5);
    const Color textSecondary = Color(0xFFA1A1AA);

    final LanguageItem srcItem = _getLangByCode(_sourceLang);
    final LanguageItem dstItem = _getLangByCode(_targetLang);

    return Scaffold(
      backgroundColor: bgDark,
      appBar: AppBar(
        backgroundColor: bgDark,
        elevation: 0,
        title: const Text(
          'TRANSLATOR STUDIO',
          style: TextStyle(
            color: textPrimary,
            fontSize: 13,
            letterSpacing: 2.0,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        children: <Widget>[
          // 语种选择控制舱
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: surfaceDark,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: borderDark),
            ),
            child: Row(
              children: <Widget>[
                // 源语言选择按钮
                Expanded(
                  child: InkWell(
                    onTap: () => _openLanguagePicker(isSource: true),
                    borderRadius: BorderRadius.circular(8),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          const Text('SOURCE / 原语言',
                              style: TextStyle(
                                  color: Color(0xFF71717A),
                                  fontSize: 9,
                                  letterSpacing: 1.0,
                                  fontWeight: FontWeight.bold)),
                          const SizedBox(height: 2),
                          Row(
                            children: <Widget>[
                              Flexible(
                                child: Text(
                                  _sourceLang == 'auto' && _detectedLangCode.isNotEmpty
                                      ? '自动 ($_detectedLangCode)'
                                      : srcItem.name,
                                  style: const TextStyle(
                                      color: textPrimary,
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const Icon(Icons.arrow_drop_down_rounded,
                                  color: textSecondary, size: 18),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                // 对调按键
                IconButton(
                  icon: const Icon(Icons.swap_horiz_rounded,
                      color: textPrimary, size: 22),
                  onPressed: _swapLanguages,
                  tooltip: '对调语种',
                ),

                // 目标语言选择按钮
                Expanded(
                  child: InkWell(
                    onTap: () => _openLanguagePicker(isSource: false),
                    borderRadius: BorderRadius.circular(8),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: <Widget>[
                          const Text('TARGET / 目标语言',
                              style: TextStyle(
                                  color: Color(0xFF71717A),
                                  fontSize: 9,
                                  letterSpacing: 1.0,
                                  fontWeight: FontWeight.bold)),
                          const SizedBox(height: 2),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: <Widget>[
                              Flexible(
                                child: Text(
                                  dstItem.name,
                                  style: const TextStyle(
                                      color: Color(0xFF38BDF8),
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const Icon(Icons.arrow_drop_down_rounded,
                                  color: Color(0xFF38BDF8), size: 18),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),

          // 常用目标语言快速点选标签
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _quickTargetLangs.map((String code) {
                final LanguageItem item = _getLangByCode(code);
                final bool isSelected = _targetLang == code;
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: ChoiceChip(
                    label: Text(item.name.split(' ')[0]),
                    selected: isSelected,
                    labelStyle: TextStyle(
                      fontSize: 11,
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      color: isSelected ? const Color(0xFF09090B) : textSecondary,
                    ),
                    selectedColor: const Color(0xFFF4F4F5),
                    backgroundColor: surfaceDark,
                    side: BorderSide(color: isSelected ? Colors.transparent : borderDark),
                    onSelected: (bool sel) {
                      if (sel && _targetLang != code) {
                        setState(() => _targetLang = code);
                        if (_inputController.text.isNotEmpty) {
                          _executeTranslation(_inputController.text.trim());
                        }
                      }
                    },
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 14),

          // 原文输入卡片
          Container(
            decoration: BoxDecoration(
              color: surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: borderDark),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
                  child: Row(
                    children: <Widget>[
                      const Text(
                        'INPUT / 原文',
                        style: TextStyle(
                            color: textSecondary,
                            fontSize: 10,
                            letterSpacing: 1.2,
                            fontWeight: FontWeight.w600),
                      ),
                      const Spacer(),
                      if (_inputController.text.isEmpty)
                        TextButton.icon(
                          style: TextButton.styleFrom(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            minimumSize: Size.zero,
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                          onPressed: _pasteFromClipboard,
                          icon: const Icon(Icons.paste_rounded, size: 14, color: textSecondary),
                          label: const Text('粘贴',
                              style: TextStyle(color: textSecondary, fontSize: 11)),
                        )
                      else
                        IconButton(
                          icon: const Icon(Icons.clear_rounded, size: 16, color: textSecondary),
                          onPressed: () => _inputController.clear(),
                          constraints: const BoxConstraints(),
                          padding: const EdgeInsets.all(4),
                        ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: TextField(
                    controller: _inputController,
                    maxLines: 5,
                    minLines: 3,
                    style: const TextStyle(color: textPrimary, fontSize: 15, height: 1.45),
                    decoration: const InputDecoration(
                      hintText: '输入或粘贴需要翻译的文本...',
                      hintStyle: TextStyle(color: Color(0xFF52525B), fontSize: 14),
                      border: InputBorder.none,
                      isDense: true,
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: Text(
                    '${_inputController.text.length} 字符',
                    style: const TextStyle(color: Color(0xFF52525B), fontSize: 11),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // 译文渲染卡片
          Container(
            decoration: BoxDecoration(
              color: const Color(0xFF121215),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: borderDark),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
                  child: Row(
                    children: <Widget>[
                      Text(
                        'RESULT / 译文 (${dstItem.name})',
                        style: const TextStyle(
                            color: Color(0xFF38BDF8),
                            fontSize: 10,
                            letterSpacing: 1.2,
                            fontWeight: FontWeight.w600),
                      ),
                      if (_isLoading) ...<Widget>[
                        const SizedBox(width: 8),
                        const SizedBox(
                          width: 10,
                          height: 10,
                          child: CircularProgressIndicator(
                              strokeWidth: 1.5, color: Color(0xFF38BDF8)),
                        ),
                      ],
                      const Spacer(),
                      if (_translatedResult.isNotEmpty)
                        IconButton(
                          icon: const Icon(Icons.copy_rounded, size: 16, color: textSecondary),
                          onPressed: _copyResult,
                          tooltip: '复制译文',
                          constraints: const BoxConstraints(),
                          padding: const EdgeInsets.all(4),
                        ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: SelectableText(
                    _translatedResult.isEmpty
                        ? (_isLoading ? '正在进行极速翻译...' : '等待输入中...')
                        : _translatedResult,
                    style: TextStyle(
                      color: _translatedResult.isEmpty
                          ? const Color(0xFF52525B)
                          : textPrimary,
                      fontSize: 15,
                      height: 1.45,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------- 全量语言搜索选择抽屉 ----------------
class _LanguagePickerSheet extends StatefulWidget {
  final bool isSource;
  final String currentSelectedCode;
  final ValueChanged<String> onSelected;

  const _LanguagePickerSheet({
    required this.isSource,
    required this.currentSelectedCode,
    required this.onSelected,
  });

  @override
  State<_LanguagePickerSheet> createState() => _LanguagePickerSheetState();
}

class _LanguagePickerSheetState extends State<_LanguagePickerSheet> {
  String _searchQuery = '';

  @override
  Widget build(BuildContext context) {
    final List<LanguageItem> candidates = kAllSupportedLanguages.where((LanguageItem l) {
      if (!widget.isSource && l.code == 'auto') return false;
      if (_searchQuery.isEmpty) return true;
      final String q = _searchQuery.toLowerCase();
      return l.name.toLowerCase().contains(q) ||
          l.nativeName.toLowerCase().contains(q) ||
          l.code.toLowerCase().contains(q);
    }).toList();

    return Container(
      height: MediaQuery.of(context).size.height * 0.72,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                widget.isSource ? '选择源语言' : '选择目标翻译语言',
                style: const TextStyle(
                    color: Color(0xFFF4F4F5), fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.close_rounded, color: Color(0xFFA1A1AA), size: 20),
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: const Color(0xFF27272A),
              borderRadius: BorderRadius.circular(10),
            ),
            child: TextField(
              onChanged: (String v) => setState(() => _searchQuery = v),
              style: const TextStyle(color: Colors.white, fontSize: 14),
              decoration: const InputDecoration(
                hintText: '搜索语言名称或代码...',
                hintStyle: TextStyle(color: Color(0xFF71717A), fontSize: 13),
                icon: Icon(Icons.search_rounded, color: Color(0xFF71717A), size: 18),
                border: InputBorder.none,
                isDense: true,
                contentPadding: EdgeInsets.symmetric(vertical: 10),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: ListView.separated(
              itemCount: candidates.length,
              separatorBuilder: (_, __) => const Divider(height: 1, color: Color(0xFF27272A)),
              itemBuilder: (BuildContext ctx, int index) {
                final LanguageItem item = candidates[index];
                final bool isSelected = item.code == widget.currentSelectedCode;

                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  title: Text(
                    item.name,
                    style: TextStyle(
                      color: isSelected ? const Color(0xFF38BDF8) : const Color(0xFFF4F4F5),
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                      fontSize: 14,
                    ),
                  ),
                  subtitle: Text(
                    item.nativeName,
                    style: const TextStyle(color: Color(0xFF71717A), fontSize: 12),
                  ),
                  trailing: isSelected
                      ? const Icon(Icons.check_rounded, color: Color(0xFF38BDF8), size: 20)
                      : null,
                  onTap: () => widget.onSelected(item.code),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
