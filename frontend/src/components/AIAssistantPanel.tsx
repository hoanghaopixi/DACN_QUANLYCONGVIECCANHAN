import { useState, useEffect, useRef } from 'react';
import {
  Sparkles, Mic, MicOff, ArrowUp, Calendar as CalendarIcon,
  Flag, AlertTriangle, BookOpen, ExternalLink, Loader2, CheckCircle2, Clock,
  Edit3, PlusCircle, ShieldAlert, MessageSquare, HelpCircle
} from 'lucide-react';
import { TimeBlock } from '../types';
import { aiService, AiParseTaskResponse } from '../services/aiService';
import { checkWorkHoursConstraint } from '../utils/workHoursConstraint';

interface AIAssistantPanelProps {
  onScheduleTask: (newTask: Partial<TimeBlock>) => Promise<any> | void;
  onEditTaskDetails: (taskInfo: any) => void;
  currentDate?: Date;
}

export default function AIAssistantPanel({
  onScheduleTask,
  onEditTaskDetails,
  currentDate,
}: AIAssistantPanelProps) {
  const [inputText, setInputText] = useState('');
  const [isListening, setIsListening] = useState(false);
  const [speechError, setSpeechError] = useState<string | null>(null);
  const [isFocused, setIsFocused] = useState(false);

  // AI response state
  const [parsedTask, setParsedTask] = useState<AiParseTaskResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [thinkingStep, setThinkingStep] = useState(0); // 0, 1, 2
  const [apiError, setApiError] = useState<string | null>(null);
  const [isConfirmed, setIsConfirmed] = useState(false);
  const [createdTask, setCreatedTask] = useState<{ id: number; title: string } | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [offHoursPendingConfirm, setOffHoursPendingConfirm] = useState<{
    useAlternative: boolean;
    reason: string;
    promptMessage: string;
  } | null>(null);

  const recognitionRef = useRef<any>(null);
  const latestTranscriptRef = useRef<string>('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const executeAiParseRef = useRef<(promptText: string) => Promise<void>>(async () => {});

  // Dynamic progressive thinking stages
  useEffect(() => {
    let t1: any, t2: any;
    if (isLoading) {
      setThinkingStep(0);
      t1 = setTimeout(() => setThinkingStep(1), 1100);
      t2 = setTimeout(() => setThinkingStep(2), 2400);
    } else {
      setThinkingStep(0);
    }
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [isLoading]);

  // Dynamic auto-grow textarea according to content
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      const scrollHeight = textareaRef.current.scrollHeight;
      textareaRef.current.style.height = `${Math.min(Math.max(scrollHeight, 46), 150)}px`;
    }
  }, [inputText]);

  // Helper: Chuyển đổi từ số nói tiếng Việt sang số (khi nói 1 thì luôn thành số 1)
  const convertSpokenWordsToNumbers = (text: string): string => {
    if (!text) return text;
    let res = text;
    // Chuyển đổi "một" thành "1" (theo yêu cầu: nói 1 thì sẽ luôn là số)
    res = res.replace(/(?:một|mốt)\s*(?:giờ|gio|h)\b/gi, '1h');
    res = res.replace(/(?:một|mốt)\s*(?:tiếng|tieng)\b/gi, '1 tiếng');
    res = res.replace(/(?:một|mốt)\s*(?:lịch|lich)\b/gi, '1 lịch');
    res = res.replace(/(?:một|mốt)\s*(?:ngày|ngay)\b/gi, '1 ngày');
    res = res.replace(/(?:một|mốt)\s*(?:phút|phut|p)\b/gi, '1 phút');
    res = res.replace(/(?:vào lúc|lúc|từ|đến|tới|khoảng|tầm)\s+(?:một|mốt)\b/gi, (match) => {
      return match.replace(/(?:một|mốt)/gi, '1');
    });
    res = res.replace(/\b(?:một|mốt)\b/gi, '1');

    // Chuyển đổi các số phổ biến khác: hai -> 2, ba -> 3...
    res = res.replace(/\bhai\s*(?:giờ|gio|h)\b/gi, '2h');
    res = res.replace(/\bhai\s*(?:tiếng|tieng)\b/gi, '2 tiếng');
    res = res.replace(/\bba\s*(?:giờ|gio|h)\b/gi, '3h');
    res = res.replace(/\bbốn\s*(?:giờ|gio|h)\b/gi, '4h');
    res = res.replace(/\bnăm\s*(?:giờ|gio|h)\b/gi, '5h');
    res = res.replace(/\bsáu\s*(?:giờ|gio|h)\b/gi, '6h');
    res = res.replace(/\bbảy\s*(?:giờ|gio|h)\b/gi, '7h');
    res = res.replace(/\btám\s*(?:giờ|gio|h)\b/gi, '8h');
    res = res.replace(/\bchín\s*(?:giờ|gio|h)\b/gi, '9h');
    res = res.replace(/\bmười\s*(?:giờ|gio|h)\b/gi, '10h');

    // Viết tắt giờ phút & thời lượng thường nói:
    res = res.replace(/\b(?:thứ|thu)\s*hai\b/gi, 'thứ 2');
    res = res.replace(/\b(?:thứ|thu)\s*ba\b/gi, 'thứ 3');
    res = res.replace(/\b(?:thứ|thu)\s*(?:tư|tu|bốn|bon)\b/gi, 'thứ 4');
    res = res.replace(/\b(?:thứ|thu)\s*(?:năm|nam)\b/gi, 'thứ 5');
    res = res.replace(/\b(?:thứ|thu)\s*(?:sáu|sau)\b/gi, 'thứ 6');
    res = res.replace(/\b(?:thứ|thu)\s*(?:bảy|bay)\b/gi, 'thứ 7');
    res = res.replace(/\b(?:chủ|chu)\s*(?:nhật|nhat)\b/gi, 'chủ nhật');
    res = res.replace(/\bmười\s*lăm\s*(?:phút|phut|p)\b/gi, '15 phút');
    res = res.replace(/\bba\s*mươi\s*(?:phút|phut|p)\b/gi, '30 phút');
    res = res.replace(/\bbốn\s*mươi\s*lăm\s*(?:phút|phut|p)\b/gi, '45 phút');
    res = res.replace(/\bnửa\s*(?:tiếng|tieng|giờ|gio)\b/gi, '30 phút');
    res = res.replace(/\bhai\s*giờ\s*ba\s*mươi(?:\s*phút)?\b/gi, '2h30');
    res = res.replace(/\bhai\s*giờ\s*rưỡi\b/gi, '2h30');
    res = res.replace(/\b(\d+)\s*tiếng\s*rưỡi\b/gi, '$1 tiếng rưỡi');

    return res;
  };

  // Initialize Web Speech API if supported
  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.lang = 'vi-VN'; // Vietnamese voice recognition

      recognition.onstart = () => {
        setIsListening(true);
        setSpeechError(null);
        latestTranscriptRef.current = '';
      };

      recognition.onresult = (event: any) => {
        let transcript = Array.from(event.results)
          .map((res: any) => res[0].transcript)
          .join('');
        transcript = convertSpokenWordsToNumbers(transcript);
        latestTranscriptRef.current = transcript;
        setInputText(transcript);
      };

      recognition.onerror = (event: any) => {
        console.warn('Speech recognition error:', event.error);
        setIsListening(false);
        if (event.error === 'not-allowed') {
          setSpeechError('Vui lòng nhấp vào biểu tượng 🔒 hoặc 🎙️ trên thanh địa chỉ trình duyệt để Cho phép (Allow) Microphone.');
        } else if (event.error === 'no-speech') {
          setSpeechError('Chưa phát hiện giọng nói. Vui lòng thử lại và nói rõ hơn.');
        } else {
          setSpeechError(`Lỗi mic (${event.error}). Bạn có thể gõ chữ trực tiếp.`);
        }
      };

      recognition.onend = () => {
        setIsListening(false);
        // Tự động phân tích ngay khi kết thúc nói
        const spoken = latestTranscriptRef.current?.trim();
        if (spoken && spoken.length > 2) {
          executeAiParseRef.current(spoken);
        }
      };

      recognitionRef.current = recognition;
    }
  }, []);

  // Execute AI Parsing
  const executeAiParse = async (promptText: string) => {
    if (!promptText.trim()) return;
    setIsLoading(true);
    setApiError(null);
    setIsConfirmed(false);
    setOffHoursPendingConfirm(null);

    try {
      let refIso: string | undefined = undefined;
      const baseDate = currentDate || new Date();
      const y = baseDate.getFullYear();
      const m = String(baseDate.getMonth() + 1).padStart(2, '0');
      const d = String(baseDate.getDate()).padStart(2, '0');
      const now = new Date();
      const hh = String(now.getHours()).padStart(2, '0');
      const mm = String(now.getMinutes()).padStart(2, '0');
      const ss = String(now.getSeconds()).padStart(2, '0');
      refIso = `${y}-${m}-${d}T${hh}:${mm}:${ss}+07:00`;
      const result = await aiService.parseTask(promptText.trim(), undefined, refIso);
      setParsedTask(result);
    } catch (err: any) {
      console.error('Lỗi phân tích AI:', err);
      setApiError('Không thể kết nối đến Trợ lý AI. Vui lòng kiểm tra Backend.');
    } finally {
      setIsLoading(false);
    }
  };

  executeAiParseRef.current = executeAiParse;

  // Handle Voice Toggle
  const toggleListening = () => {
    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      const spoken = latestTranscriptRef.current?.trim();
      if (spoken && spoken.length > 2) {
        executeAiParseRef.current(spoken);
      }
    } else {
      if (recognitionRef.current) {
        try {
          setSpeechError(null);
          latestTranscriptRef.current = '';
          recognitionRef.current.start();
        } catch (e) {
          recognitionRef.current.stop();
          setTimeout(() => recognitionRef.current?.start(), 150);
        }
      } else {
        setIsListening(true);
        setSpeechError('Trình duyệt chưa hỗ trợ Web Speech trực tiếp. Đang giả lập ghi âm...');
        setTimeout(() => {
          const sample = 'Nhắc tôi tập thể dục 30 phút vào tối nay lúc 7h';
          setInputText(sample);
          setIsListening(false);
          setSpeechError(null);
          executeAiParseRef.current(sample);
        }, 2500);
      }
    }
  };


  // Handle Send button click
  const handleSend = () => {
    executeAiParse(inputText);
  };

  // Handle Confirm & Auto-reschedule or direct schedule
  const handleConfirmSchedule = async (useAlternative: boolean = false, bypassOffHours: boolean = false) => {
    if (!parsedTask || isSubmitting) return;

    const taskList = (parsedTask.tasks && parsedTask.tasks.length > 0)
      ? parsedTask.tasks
      : [parsedTask];

    if (taskList.length > 1) {
      setIsSubmitting(true);
      try {
        for (const t of taskList) {
          await onScheduleTask({
            title: t.title,
            note: t.note,
            description: t.note,
            startTime: t.startTime,
            endTime: t.endTime,
            categoryType: t.categoryType,
            status: 'TODO',
            isAutoScheduled: true,
            priority: t.priority,
          });
        }
        setOffHoursPendingConfirm(null);
        setIsConfirmed(true);
      } catch (e) {
        console.error('Lỗi khi thêm nhiều công việc vào lịch:', e);
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    const start = (useAlternative && parsedTask.suggestedAlternativeStartTime)
      ? parsedTask.suggestedAlternativeStartTime
      : parsedTask.startTime;

    const end = (useAlternative && parsedTask.suggestedAlternativeEndTime)
      ? parsedTask.suggestedAlternativeEndTime
      : parsedTask.endTime;

    // Ràng buộc kiểm tra ngoài giờ làm việc & giờ nghỉ (đã được tối ưu thân thiện)
    if (!bypassOffHours) {
      const val = checkWorkHoursConstraint(start, end, parsedTask.categoryType);
      if (val.isOutsideWorkHours) {
        setOffHoursPendingConfirm({
          useAlternative,
          reason: val.reason || 'Lưu ý về khung giờ sinh hoạt và làm việc.',
          promptMessage: val.promptMessage || 'Bạn có muốn tiếp tục lưu lịch vào khung giờ này không?',
        });
        return;
      }
    }

    setIsSubmitting(true);
    try {
      const res: any = await onScheduleTask({
        title: parsedTask.title,
        note: parsedTask.note,
        description: parsedTask.note,
        startTime: start,
        endTime: end,
        categoryType: parsedTask.categoryType,
        status: 'TODO',
        isAutoScheduled: true,
        priority: parsedTask.priority,
      });

      if (res && (res.id || res.taskId)) {
        setCreatedTask({ id: res.id || res.taskId, title: parsedTask.title });
      }
      setOffHoursPendingConfirm(null);
      setIsConfirmed(true);
    } catch (e) {
      console.error('Lỗi khi thêm vào lịch từ AI:', e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditCreatedTask = () => {
    if (!parsedTask) return;
    const now = new Date();
    const defaultDateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    const datePart = parsedTask.startTime ? parsedTask.startTime.substring(0, 10) : defaultDateStr;
    const startH = parsedTask.startTime ? parsedTask.startTime.substring(11, 16) : '15:00';
    const endH = parsedTask.endTime ? parsedTask.endTime.substring(11, 16) : '16:30';

    onEditTaskDetails({
      id: createdTask?.id, // TRUYỀN ID CỤ THỂ để update, KHÔNG tạo công việc trùng lặp!
      title: parsedTask.title,
      date: datePart,
      startTime: startH,
      endTime: endH,
      priority: parsedTask.priority,
      categoryType: parsedTask.categoryType,
      estimatedDurationMinutes: parsedTask.estimatedDurationMinutes,
    });
  };

  const handleStartNewTask = () => {
    setParsedTask(null);
    setInputText('');
    setIsConfirmed(false);
    setCreatedTask(null);
    setOffHoursPendingConfirm(null);
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  };

  const getPriorityLabel = (pri?: string) => {
    switch (pri) {
      case 'URGENT': return 'Khẩn cấp';
      case 'HIGH': return 'Cao';
      case 'LOW': return 'Thấp';
      default: return 'Trung bình';
    }
  };

  const getPriorityColor = (pri?: string) => {
    switch (pri) {
      case 'URGENT': return '#DC2626';
      case 'HIGH': return '#EA580C';
      case 'LOW': return '#16A34A';
      default: return '#2563EB';
    }
  };

  const getCategoryLabel = (cat?: string) => {
    switch (cat?.toUpperCase()) {
      case 'STUDY': return 'HỌC TẬP';
      case 'HEALTH': return 'SỨC KHỎE';
      case 'ENTERTAINMENT': return 'GIẢI TRÍ';
      case 'WORK':
      default: return 'CÔNG VIỆC';
    }
  };

  return (
    <aside className="ai-panel">
      {/* ── Header ── */}
      <div className="ai-header">
        <div className="ai-header-left">
          <div className="ai-icon">
            <Sparkles size={20} />
          </div>
          <h3>Trợ lý SmartTask</h3>
        </div>
        <span className="ai-status">Sẵn sàng</span>
      </div>

      <p className="ai-welcome">
        Bạn nói điều cần làm. Mình tìm thời gian phù hợp, bạn quyết định.
      </p>

      {/* ── Input Card with Mic & Send ── */}
      <div style={{ padding: '0 20px 14px' }}>
        <div
          style={{
            background: '#FFFFFF',
            border: isListening
              ? '1.5px solid #EF4444'
              : isFocused
                ? '1.5px solid #6C5CE7'
                : '1px solid #E2E8F0',
            borderRadius: 12,
            padding: '12px 14px 10px',
            boxShadow: isListening
              ? '0 0 0 3px rgba(239, 68, 68, 0.15)'
              : isFocused
                ? '0 0 0 3px rgba(108, 92, 231, 0.12)'
                : '0 1px 3px rgba(0,0,0,0.05)',
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
            transition: 'all 0.2s ease',
          }}
        >
          {/* Text input area with clear button */}
          <div style={{ position: 'relative', width: '100%' }}>
            <textarea
              ref={textareaRef}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  if (!isLoading && inputText.trim()) {
                    handleSend();
                  }
                }
              }}
              rows={2}
              placeholder="Nói hoặc gõ công việc cần làm... (Ví dụ: Đọc sách sáng mai lúc 8h)"
              style={{
                width: '100%',
                border: 'none',
                outline: 'none',
                resize: 'none',
                fontSize: '0.88rem',
                fontFamily: 'inherit',
                color: '#1E293B',
                lineHeight: 1.5,
                background: 'transparent',
                paddingRight: inputText ? 28 : 0,
                boxSizing: 'border-box',
                overflowY: 'auto',
              }}
            />

            {/* Quick clear button (✕) */}
            {inputText && (
              <button
                type="button"
                onClick={() => {
                  setInputText('');
                  textareaRef.current?.focus();
                }}
                style={{
                  position: 'absolute',
                  top: 0,
                  right: 0,
                  background: '#F1F5F9',
                  border: 'none',
                  borderRadius: '50%',
                  width: 20,
                  height: 20,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                  color: '#64748B',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  transition: 'all 0.15s',
                }}
                title="Xóa nhanh (Clear)"
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#E2E8F0';
                  e.currentTarget.style.color = '#0F172A';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#F1F5F9';
                  e.currentTarget.style.color = '#64748B';
                }}
              >
                ✕
              </button>
            )}
          </div>

          {/* Speech recording banner if listening */}
          {isListening && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                fontSize: '0.78rem',
                color: '#DC2626',
                background: '#FEF2F2',
                padding: '6px 10px',
                borderRadius: 8,
                fontWeight: 600,
                border: '1px solid #FECACA',
              }}
            >
              <span
                style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  background: '#DC2626',
                  animation: 'pulse-mic 1s infinite',
                  flexShrink: 0,
                }}
              />
              <span style={{ flex: 1 }}>Đang lắng nghe giọng nói tiếng Việt... Hãy nói câu lệnh</span>
              <button
                type="button"
                onClick={toggleListening}
                style={{
                  border: 'none',
                  background: '#DC2626',
                  color: '#FFFFFF',
                  borderRadius: 6,
                  padding: '2px 8px',
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Dừng & Gửi
              </button>
            </div>
          )}

          {speechError && (
            <div
              style={{
                fontSize: '0.74rem',
                color: '#DC2626',
                background: '#FEF2F2',
                border: '1px solid #FECACA',
                borderRadius: 8,
                padding: '7px 10px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 8,
                lineHeight: 1.4,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}>
                <AlertTriangle size={14} style={{ color: '#DC2626', flexShrink: 0 }} />
                <span>{speechError}</span>
              </div>
              <button
                type="button"
                onClick={() => setSpeechError(null)}
                style={{
                  border: 'none',
                  background: 'transparent',
                  color: '#991B1B',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  padding: '0 2px',
                  lineHeight: 1,
                }}
                title="Đóng thông báo"
              >
                ✕
              </button>
            </div>
          )}

          {/* Bottom actions row: Mic button + Send button */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: 4,
            }}
          >
            {/* Mic button with text matching mockup */}
            <button
              onClick={toggleListening}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                padding: '7px 12px',
                borderRadius: 8,
                fontSize: '0.82rem',
                color: isListening ? '#DC2626' : '#475569',
                background: isListening ? '#FEF2F2' : '#F8FAFC',
                border: '1px solid',
                borderColor: isListening ? '#FCA5A5' : '#E2E8F0',
                fontWeight: 500,
                transition: 'all 0.2s',
                cursor: 'pointer',
              }}
              title={isListening ? 'Nhấn để dừng thu âm' : 'Nhấn để nói tiếng Việt bằng Web Speech API'}
            >
              {isListening ? (
                <>
                  <MicOff size={15} style={{ color: '#DC2626' }} />
                  <span>Dừng thu âm</span>
                </>
              ) : (
                <>
                  <Mic size={15} style={{ color: '#6C5CE7' }} />
                  <span>Nhập giọng nói</span>
                </>
              )}
            </button>

            {/* Send button */}
            <button
              onClick={handleSend}
              disabled={isLoading || !inputText.trim()}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 5,
                padding: '7px 18px',
                background: isLoading ? '#94A3B8' : !inputText.trim() ? '#E2E8F0' : '#6C5CE7',
                color: !inputText.trim() && !isLoading ? '#94A3B8' : '#FFFFFF',
                borderRadius: 8,
                fontSize: '0.83rem',
                fontWeight: 600,
                boxShadow: !inputText.trim() || isLoading ? 'none' : '0 1px 3px rgba(108,92,231,0.3)',
                cursor: isLoading || !inputText.trim() ? 'not-allowed' : 'pointer',
                border: 'none',
                transition: 'all 0.2s',
              }}
              title="Nhấn để phân tích và lập lịch"
            >
              {isLoading ? (
                <Loader2 size={15} className="animate-spin" />
              ) : (
                <ArrowUp size={15} strokeWidth={2.5} />
              )}
              <span>{isLoading ? 'Đang phân tích...' : 'Gửi'}</span>
            </button>
          </div>
        </div>

        {/* Input helper shortcut hint */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.7rem',
            color: '#94A3B8',
            marginTop: 5,
            padding: '0 4px',
          }}
        >
          <span>💡 Nhấn <b>Enter</b> để gửi, <b>Shift+Enter</b> xuống dòng</span>
          {inputText.trim() && <span>{inputText.trim().length} ký tự</span>}
        </div>
      </div>

      {/* ── AI Thinking & Shimmering Transition State ── */}
      {isLoading && (
        <div
          style={{
            margin: '0 20px',
            padding: '20px',
            background: 'linear-gradient(135deg, rgba(255,255,255,0.95), rgba(248,250,252,0.95))',
            border: '1.5px solid rgba(108, 92, 231, 0.35)',
            borderRadius: 16,
            animation: 'aiPulseGlow 2.5s infinite ease-in-out',
            display: 'flex',
            flexDirection: 'column',
            gap: 14,
          }}
        >
          {/* Top thinking indicator */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 34,
                height: 34,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #6C5CE7, #a29bfe)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#FFFFFF',
                boxShadow: '0 4px 10px rgba(108,92,231,0.3)',
                flexShrink: 0,
              }}
            >
              <Sparkles size={18} className="animate-spin" style={{ animationDuration: '3s' }} />
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
              <div
                style={{
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  color: '#1E293B',
                  animation: 'thoughtFadeIn 0.35s ease-out',
                }}
                key={thinkingStep}
              >
                {thinkingStep === 0 && 'Đang phân tích câu lệnh tiếng Việt...'}
                {thinkingStep === 1 && 'Đang đối chiếu lịch tuần của bạn...'}
                {thinkingStep === 2 && 'Đang hoàn thiện đề xuất lịch trình...'}
              </div>
              <div
                style={{
                  fontSize: '0.72rem',
                  color: '#64748B',
                  marginTop: 2,
                  animation: 'thoughtFadeIn 0.35s ease-out',
                }}
                key={`sub-${thinkingStep}`}
              >
                {thinkingStep === 0 && 'Trích xuất nội dung, thể loại và dải giờ'}
                {thinkingStep === 1 && 'Kiểm tra xung đột & tìm khung giờ năng suất nhất'}
                {thinkingStep === 2 && 'Sắp xếp thông minh & chuẩn bị thẻ xác nhận'}
              </div>
            </div>

            {/* Bouncing dots */}
            <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#6C5CE7', animation: 'dotPulse 1.4s infinite ease-in-out 0s' }} />
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#6C5CE7', animation: 'dotPulse 1.4s infinite ease-in-out 0.2s' }} />
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#6C5CE7', animation: 'dotPulse 1.4s infinite ease-in-out 0.4s' }} />
            </div>
          </div>

          {/* Skeleton Shimmer Preview Placeholder */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, opacity: 0.75 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div
                style={{
                  width: 80,
                  height: 18,
                  borderRadius: 12,
                  background: 'linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%)',
                  backgroundSize: '200% 100%',
                  animation: 'aiShimmer 1.8s infinite',
                }}
              />
              <div
                style={{
                  width: 65,
                  height: 18,
                  borderRadius: 12,
                  background: 'linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%)',
                  backgroundSize: '200% 100%',
                  animation: 'aiShimmer 1.8s infinite',
                }}
              />
            </div>
            <div
              style={{
                width: '70%',
                height: 22,
                borderRadius: 6,
                background: 'linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%)',
                backgroundSize: '200% 100%',
                animation: 'aiShimmer 1.8s infinite',
              }}
            />
            <div
              style={{
                width: '50%',
                height: 14,
                borderRadius: 4,
                background: 'linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%)',
                backgroundSize: '200% 100%',
                animation: 'aiShimmer 1.8s infinite',
              }}
            />
          </div>
        </div>
      )}

      {/* ── Error Banner ── */}
      {apiError && !isLoading && (
        <div
          style={{
            margin: '0 20px',
            padding: '12px',
            background: '#FEF2F2',
            border: '1px solid #FECACA',
            borderRadius: 10,
            color: '#B91C1C',
            fontSize: '0.8rem',
          }}
        >
          {apiError}
        </div>
      )}

      {/* ── AI Response / Feedback / Proposal Card ── */}
      {parsedTask && !isLoading && (
        parsedTask.isValidTask === false ? (
          <div
            style={{
              padding: '0 20px',
              display: 'flex',
              flexDirection: 'column',
              gap: 12,
              animation: 'cardSpringEnter 0.45s cubic-bezier(0.16, 1, 0.3, 1) forwards',
            }}
          >
            {/* Header */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                fontSize: '0.86rem',
                fontWeight: 700,
                color: parsedTask.validationType === 'INAPPROPRIATE'
                  ? '#DC2626'
                  : parsedTask.validationType === 'GREETING'
                  ? '#4F46E5'
                  : '#D97706',
              }}
            >
              <div
                style={{
                  width: 26,
                  height: 26,
                  borderRadius: 7,
                  background: parsedTask.validationType === 'INAPPROPRIATE'
                    ? '#FEE2E2'
                    : parsedTask.validationType === 'GREETING'
                    ? '#EEF2FF'
                    : '#FEF3C7',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: parsedTask.validationType === 'INAPPROPRIATE'
                    ? '#DC2626'
                    : parsedTask.validationType === 'GREETING'
                    ? '#4F46E5'
                    : '#D97706',
                }}
              >
                {parsedTask.validationType === 'INAPPROPRIATE' ? (
                  <ShieldAlert size={15} />
                ) : parsedTask.validationType === 'GREETING' ? (
                  <MessageSquare size={15} />
                ) : (
                  <HelpCircle size={15} />
                )}
              </div>
              <span>
                {parsedTask.validationType === 'INAPPROPRIATE'
                  ? 'Nội dung không phù hợp'
                  : parsedTask.validationType === 'GREETING'
                  ? 'Trợ lý SmartTask AI'
                  : 'Chưa đủ thông tin công việc'}
              </span>
            </div>

            {/* Content feedback box */}
            <div
              style={{
                background: parsedTask.validationType === 'INAPPROPRIATE'
                  ? '#FEF2F2'
                  : parsedTask.validationType === 'GREETING'
                  ? '#F8FAFC'
                  : '#FFFBEB',
                border: `1.5px solid ${
                  parsedTask.validationType === 'INAPPROPRIATE'
                    ? '#FECACA'
                    : parsedTask.validationType === 'GREETING'
                    ? '#E2E8F0'
                    : '#FDE68A'
                }`,
                borderRadius: 14,
                padding: '16px',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
                boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
              }}
            >
              <p
                style={{
                  margin: 0,
                  fontSize: '0.84rem',
                  lineHeight: 1.55,
                  color: parsedTask.validationType === 'INAPPROPRIATE'
                    ? '#991B1B'
                    : parsedTask.validationType === 'GREETING'
                    ? '#334155'
                    : '#92400E',
                  fontWeight: 500,
                }}
              >
                {parsedTask.validationMessage ||
                  'Nội dung chưa đủ cụ thể để lên lịch trình. Vui lòng nhập việc cần làm kèm thời gian bạn nhé.'}
              </p>

              {/* Sample quick-pick chips if greeting or nonsense */}
              {parsedTask.validationType !== 'INAPPROPRIATE' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 2 }}>
                  <div style={{ fontSize: '0.74rem', fontWeight: 600, color: '#64748B' }}>
                    💡 Thử bấm chọn mẫu để SmartTask lên lịch ngay:
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {[
                      { text: 'Học tiếng Anh 1 tiếng lúc 20h tối mai', icon: '📚' },
                      { text: 'Chạy bộ 30 phút vào 6h sáng mai', icon: '🏃' },
                      { text: 'Họp nhóm đồ án lúc 9h sáng thứ 6', icon: '💻' },
                    ].map((item, idx) => (
                      <button
                        key={idx}
                        onClick={() => {
                          setInputText(item.text);
                          executeAiParse(item.text);
                        }}
                        style={{
                          padding: '8px 12px',
                          background: '#FFFFFF',
                          border: '1px solid #E2E8F0',
                          borderRadius: 8,
                          fontSize: '0.78rem',
                          color: '#4338CA',
                          fontWeight: 600,
                          textAlign: 'left',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                          transition: 'all 0.15s ease',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.borderColor = '#6366F1';
                          e.currentTarget.style.background = '#EEF2FF';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.borderColor = '#E2E8F0';
                          e.currentTarget.style.background = '#FFFFFF';
                        }}
                      >
                        <span>{item.icon}</span>
                        <span style={{ flex: 1 }}>{item.text}</span>
                        <span style={{ fontSize: '0.72rem', color: '#6366F1' }}>Lên lịch →</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div
            style={{
              padding: '0 20px',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
              animation: 'cardSpringEnter 0.45s cubic-bezier(0.16, 1, 0.3, 1) forwards',
            }}
          >
          {/* Header divider */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              fontSize: '0.86rem',
              fontWeight: 700,
              color: '#1E293B',
            }}
          >
            <div
              style={{
                width: 24,
                height: 24,
                borderRadius: 6,
                background: '#EEF2FF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#6C5CE7',
              }}
            >
              <Sparkles size={14} />
            </div>
            <span>
              {parsedTask.tasks && parsedTask.tasks.length > 1
                ? `Đề xuất ${parsedTask.tasks.length} lịch trình phù hợp`
                : 'Đề xuất lịch trình phù hợp'}
            </span>
            {parsedTask.tasks && parsedTask.tasks.length > 1 && (
              <span
                style={{
                  marginLeft: 'auto',
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  color: '#6C5CE7',
                  background: '#EEF2FF',
                  padding: '2px 8px',
                  borderRadius: 10,
                }}
              >
                {parsedTask.tasks.length} công việc
              </span>
            )}
          </div>

          {parsedTask.tasks && parsedTask.tasks.length > 1 ? (
            /* ── Multi-Task Stacked Cards ── */
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {parsedTask.tasks.map((taskItem, idx) => (
                <div
                  key={idx}
                  style={{
                    background: '#F8FAFC',
                    border: '1px solid #E2E8F0',
                    borderRadius: 14,
                    padding: '14px 16px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 8,
                    boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
                    transition: 'all 0.2s ease',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span
                        style={{
                          fontSize: '0.72rem',
                          fontWeight: 700,
                          color: '#4F46E5',
                          background: '#EEF2FF',
                          padding: '2px 7px',
                          borderRadius: 6,
                        }}
                      >
                        #{idx + 1}
                      </span>
                      <div
                        style={{
                          fontSize: '0.72rem',
                          color: taskItem.categoryType === 'STUDY' ? '#7C3AED' : taskItem.categoryType === 'HEALTH' ? '#059669' : taskItem.categoryType === 'ENTERTAINMENT' ? '#D97706' : '#2563EB',
                          fontWeight: 700,
                          letterSpacing: '0.04em',
                          background: taskItem.categoryType === 'STUDY' ? '#F5F3FF' : taskItem.categoryType === 'HEALTH' ? '#ECFDF5' : taskItem.categoryType === 'ENTERTAINMENT' ? '#FFFBEB' : '#EFF6FF',
                          padding: '3px 9px',
                          borderRadius: 12,
                        }}
                      >
                        {getCategoryLabel(taskItem.categoryType)}
                      </div>
                    </div>
                    <div
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 600,
                        color: getPriorityColor(taskItem.priority),
                        background: '#FFFFFF',
                        padding: '2px 8px',
                        borderRadius: 12,
                        border: '1px solid #E2E8F0',
                      }}
                    >
                      {getPriorityLabel(taskItem.priority)}
                    </div>
                  </div>

                  <div style={{ fontSize: '1rem', fontWeight: 700, color: '#0F172A', marginTop: 2 }}>
                    {taskItem.title}
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.82rem', color: '#475569' }}>
                    <CalendarIcon size={14} style={{ color: '#6C5CE7', flexShrink: 0 }} />
                    <span style={{ fontWeight: 500 }}>{taskItem.formattedDateLabel}</span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.82rem', color: '#475569' }}>
                    <Flag size={14} style={{ color: '#64748B', flexShrink: 0 }} />
                    <span>Thời lượng ước tính: {taskItem.estimatedDurationMinutes} phút</span>
                  </div>

                  {taskItem.note && (
                    <p style={{ fontSize: '0.76rem', color: '#64748B', lineHeight: 1.45, margin: '2px 0 0 0', fontStyle: 'italic' }}>
                      💬 {taskItem.note}
                    </p>
                  )}

                  {/* Conflict warning for individual task */}
                  {taskItem.hasConflict && (
                    <div
                      style={{
                        marginTop: 4,
                        background: '#FFF7ED',
                        border: '1px solid #FED7AA',
                        borderRadius: 8,
                        padding: '8px 10px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 3,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#C2410C', fontWeight: 600, fontSize: '0.78rem' }}>
                        <AlertTriangle size={14} />
                        <span>Khung giờ này đang bận</span>
                      </div>
                      <p style={{ fontSize: '0.74rem', color: '#9A3412', lineHeight: 1.4, margin: 0 }}>
                        {taskItem.conflictMessage}
                      </p>
                    </div>
                  )}
                </div>
              ))}

              {/* Master Confirmation or Action for multi-task */}
              {isConfirmed ? (
                <div
                  style={{
                    padding: '14px',
                    background: 'linear-gradient(135deg, #ECFDF5, #F0FDF4)',
                    border: '1.5px solid #A7F3D0',
                    borderRadius: 12,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 10,
                    animation: 'cardSpringEnter 0.35s ease-out',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#065F46' }}>
                    <CheckCircle2 size={18} style={{ color: '#10B981', flexShrink: 0 }} />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.86rem' }}>
                        Đã lưu an toàn cả {parsedTask.tasks.length} công việc vào lịch!
                      </div>
                      <div style={{ fontSize: '0.74rem', color: '#047857' }}>
                        Tất cả các công việc đã hiển thị trên bảng thời gian của bạn.
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={handleStartNewTask}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      padding: '9px 12px',
                      background: '#6C5CE7',
                      border: 'none',
                      borderRadius: 8,
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: '#FFFFFF',
                      cursor: 'pointer',
                      boxShadow: '0 2px 4px rgba(108,92,231,0.2)',
                    }}
                  >
                    <PlusCircle size={14} />
                    <span>Lên lịch công việc khác</span>
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
                  <button
                    onClick={() => handleConfirmSchedule(false)}
                    disabled={isSubmitting}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 8,
                      padding: '12px 16px',
                      background: 'linear-gradient(135deg, #4F46E5, #6C5CE7)',
                      color: '#FFFFFF',
                      borderRadius: 10,
                      fontWeight: 600,
                      fontSize: '0.88rem',
                      boxShadow: '0 3px 8px rgba(79,70,229,0.3)',
                      cursor: isSubmitting ? 'not-allowed' : 'pointer',
                      opacity: isSubmitting ? 0.75 : 1,
                      border: 'none',
                      transition: 'all 0.2s',
                    }}
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        <span>Đang lưu cả {parsedTask.tasks.length} việc vào lịch...</span>
                      </>
                    ) : (
                      <>
                        <span>→ Xác nhận & thêm cả {parsedTask.tasks.length} việc vào lịch</span>
                      </>
                    )}
                  </button>

                  <button
                    onClick={handleStartNewTask}
                    style={{
                      textAlign: 'center',
                      color: '#64748B',
                      fontSize: '0.8rem',
                      fontWeight: 500,
                      padding: '6px 0',
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                    }}
                  >
                    Hủy bỏ đề xuất này
                  </button>
                </div>
              )}
            </div>
          ) : (
            /* Single Details Card */
            <div
              style={{
                background: '#F8FAFC',
                border: '1px solid #E2E8F0',
                borderRadius: 14,
                padding: '16px',
                display: 'flex',
                flexDirection: 'column',
                gap: 10,
                boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div
                  style={{
                    fontSize: '0.72rem',
                    color: parsedTask.categoryType === 'STUDY' ? '#7C3AED' : parsedTask.categoryType === 'HEALTH' ? '#059669' : parsedTask.categoryType === 'ENTERTAINMENT' ? '#D97706' : '#2563EB',
                    fontWeight: 700,
                    letterSpacing: '0.04em',
                    background: parsedTask.categoryType === 'STUDY' ? '#F5F3FF' : parsedTask.categoryType === 'HEALTH' ? '#ECFDF5' : parsedTask.categoryType === 'ENTERTAINMENT' ? '#FFFBEB' : '#EFF6FF',
                    padding: '3px 9px',
                    borderRadius: 12,
                  }}
                >
                  {getCategoryLabel(parsedTask.categoryType)}
                </div>
                <div
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 600,
                    color: getPriorityColor(parsedTask.priority),
                    background: '#FFFFFF',
                    padding: '2px 8px',
                    borderRadius: 12,
                    border: '1px solid #E2E8F0',
                  }}
                >
                  {getPriorityLabel(parsedTask.priority)}
                </div>
              </div>

              <div style={{ fontSize: '1.02rem', fontWeight: 700, color: '#0F172A', marginTop: 2 }}>
                {parsedTask.title}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.82rem', color: '#475569' }}>
                <CalendarIcon size={14} style={{ color: '#6C5CE7' }} />
                <span style={{ fontWeight: 500 }}>{parsedTask.formattedDateLabel}</span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.82rem', color: '#475569' }}>
                <Flag size={14} style={{ color: '#64748B' }} />
                <span>Thời lượng ước tính: {parsedTask.estimatedDurationMinutes} phút</span>
              </div>

              {parsedTask.note && (
                <p style={{ fontSize: '0.76rem', color: '#64748B', lineHeight: 1.45, margin: '2px 0 0 0', fontStyle: 'italic' }}>
                  💬 {parsedTask.note}
                </p>
              )}

              {/* Lưu ý ngoài giờ làm việc & giờ nghỉ (chỉ hiển thị khi thực sự cần) */}
              {(() => {
                const check = checkWorkHoursConstraint(parsedTask.startTime, parsedTask.endTime, parsedTask.categoryType);
                if (!check.isOutsideWorkHours) return null;
                return (
                  <div
                    style={{
                      marginTop: 4,
                      background: '#F8FAFC',
                      border: '1px solid #E2E8F0',
                      borderLeft: '3px solid #F59E0B',
                      borderRadius: 8,
                      padding: '8px 12px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 3,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#B45309', fontWeight: 600, fontSize: '0.8rem' }}>
                      <Clock size={14} />
                      <span>{check.promptTitle}</span>
                    </div>
                    <p style={{ fontSize: '0.75rem', color: '#475569', lineHeight: 1.4, margin: 0 }}>
                      {check.reason}
                    </p>
                  </div>
                );
              })()}

              {/* Amber Conflict Warning */}
              {parsedTask.hasConflict && (
                <div
                  style={{
                    marginTop: 6,
                    background: '#FFF7ED',
                    border: '1px solid #FED7AA',
                    borderRadius: 8,
                    padding: '10px 12px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 4,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#C2410C', fontWeight: 600, fontSize: '0.82rem' }}>
                    <AlertTriangle size={15} />
                    <span>Khung giờ này đang bận</span>
                  </div>
                  <p style={{ fontSize: '0.77rem', color: '#9A3412', lineHeight: 1.45, margin: 0 }}>
                    {parsedTask.conflictMessage}
                  </p>
                </div>
              )}

              {/* Hộp xác nhận khi tạo việc ngoài giờ nghỉ */}
              {offHoursPendingConfirm && (
                <div
                  style={{
                    marginTop: 8,
                    background: '#FFFBEB',
                    border: '1.5px solid #FCD34D',
                    borderRadius: 10,
                    padding: '12px 14px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 8,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#B45309', fontWeight: 700, fontSize: '0.85rem' }}>
                    <Clock size={16} />
                    <span>{offHoursPendingConfirm.promptMessage}</span>
                  </div>
                  <p style={{ fontSize: '0.78rem', color: '#78350F', margin: 0 }}>
                    {offHoursPendingConfirm.reason}
                  </p>
                  <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                    <button
                      onClick={() => setOffHoursPendingConfirm(null)}
                      style={{
                        flex: 1,
                        padding: '7px 10px',
                        background: '#FFFFFF',
                        border: '1px solid #D1D5DB',
                        borderRadius: 6,
                        fontSize: '0.8rem',
                        fontWeight: 600,
                        color: '#4B5563',
                        cursor: 'pointer',
                      }}
                    >
                      Hủy bỏ
                    </button>
                    <button
                      onClick={() => handleConfirmSchedule(offHoursPendingConfirm.useAlternative, true)}
                      style={{
                        flex: 1,
                        padding: '7px 10px',
                        background: '#D97706',
                        border: 'none',
                        borderRadius: 6,
                        fontSize: '0.8rem',
                        fontWeight: 600,
                        color: '#FFFFFF',
                        cursor: 'pointer',
                      }}
                    >
                      Tiếp tục lưu lịch
                    </button>
                  </div>
                </div>
              )}

              {/* ── Action State: Once Confirmed ── */}
              {isConfirmed ? (
                <div
                  style={{
                    marginTop: 8,
                    padding: '14px',
                    background: 'linear-gradient(135deg, #ECFDF5, #F0FDF4)',
                    border: '1.5px solid #A7F3D0',
                    borderRadius: 12,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 10,
                    animation: 'cardSpringEnter 0.35s ease-out',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#065F46' }}>
                    <CheckCircle2 size={18} style={{ color: '#10B981', flexShrink: 0 }} />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.86rem' }}>Đã lưu an toàn vào lịch!</div>
                      <div style={{ fontSize: '0.74rem', color: '#047857' }}>Công việc đã hiển thị trên bảng thời gian của bạn.</div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: 8, marginTop: 2 }}>
                    <button
                      onClick={handleEditCreatedTask}
                      style={{
                        flex: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 6,
                        padding: '8px 10px',
                        background: '#FFFFFF',
                        border: '1px solid #D1D5DB',
                        borderRadius: 8,
                        fontSize: '0.78rem',
                        fontWeight: 600,
                        color: '#374151',
                        cursor: 'pointer',
                        transition: 'all 0.15s',
                      }}
                      title="Chỉnh sửa công việc này (không tạo trùng lặp)"
                    >
                      <Edit3 size={13} style={{ color: '#6C5CE7' }} />
                      <span>Chỉnh sửa việc này</span>
                    </button>

                    <button
                      onClick={handleStartNewTask}
                      style={{
                        flex: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 6,
                        padding: '8px 10px',
                        background: '#6C5CE7',
                        border: 'none',
                        borderRadius: 8,
                        fontSize: '0.78rem',
                        fontWeight: 600,
                        color: '#FFFFFF',
                        cursor: 'pointer',
                        boxShadow: '0 2px 4px rgba(108,92,231,0.2)',
                      }}
                    >
                      <PlusCircle size={13} />
                      <span>Tạo việc khác</span>
                    </button>
                  </div>
                </div>
              ) : !offHoursPendingConfirm ? (
                /* ── Action State: Not Confirmed Yet ── */
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 6 }}>
                  {parsedTask.hasConflict ? (
                    <>
                      {/* Nút chính ưu tiên: Tự động chuyển sang giờ trống tiếp theo */}
                      {parsedTask.suggestedAlternativeStartTime && (
                        <button
                          onClick={() => handleConfirmSchedule(true)}
                          disabled={isSubmitting}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 6,
                            padding: '10px 14px',
                            background: '#4F46E5',
                            color: '#FFFFFF',
                            borderRadius: 8,
                            fontWeight: 600,
                            fontSize: '0.86rem',
                            boxShadow: '0 2px 6px rgba(79,70,229,0.25)',
                            cursor: isSubmitting ? 'not-allowed' : 'pointer',
                            opacity: isSubmitting ? 0.75 : 1,
                            border: 'none',
                          }}
                        >
                          {isSubmitting ? <Loader2 size={15} className="animate-spin" /> : <span>⚡</span>}
                          <span>Chuyển sang giờ trống ({parsedTask.suggestedAlternativeStartTime.substring(11, 16)}–{parsedTask.suggestedAlternativeEndTime?.substring(11, 16)})</span>
                        </button>
                      )}

                      {/* Nút phụ: Giữ nguyên giờ trùng */}
                      <button
                        onClick={() => handleConfirmSchedule(false)}
                        disabled={isSubmitting}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: 6,
                          padding: '8px 12px',
                          background: '#FFF7ED',
                          color: '#C2410C',
                          border: '1px solid #FED7AA',
                          borderRadius: 8,
                          fontWeight: 600,
                          fontSize: '0.8rem',
                          cursor: isSubmitting ? 'not-allowed' : 'pointer',
                        }}
                      >
                        <span>Vẫn giữ giờ này ({parsedTask.startTime.substring(11, 16)}–{parsedTask.endTime.substring(11, 16)})</span>
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => handleConfirmSchedule(false)}
                      disabled={isSubmitting}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 8,
                        padding: '11px 16px',
                        background: 'linear-gradient(135deg, #4F46E5, #6C5CE7)',
                        color: '#FFFFFF',
                        borderRadius: 10,
                        fontWeight: 600,
                        fontSize: '0.88rem',
                        boxShadow: '0 3px 8px rgba(79,70,229,0.3)',
                        cursor: isSubmitting ? 'not-allowed' : 'pointer',
                        opacity: isSubmitting ? 0.75 : 1,
                        border: 'none',
                        transition: 'all 0.2s',
                      }}
                    >
                      {isSubmitting ? (
                        <>
                          <Loader2 size={16} className="animate-spin" />
                          <span>Đang lưu vào lịch...</span>
                        </>
                      ) : (
                        <>
                          <span>→ Xác nhận & thêm vào lịch</span>
                        </>
                      )}
                    </button>
                  )}

                  {/* Secondary Action: Edit before confirm */}
                  <button
                    onClick={() => {
                      const now = new Date();
                      const defaultDateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
                      const datePart = parsedTask.startTime ? parsedTask.startTime.substring(0, 10) : defaultDateStr;
                      const startH = parsedTask.startTime ? parsedTask.startTime.substring(11, 16) : '15:00';
                      const endH = parsedTask.endTime ? parsedTask.endTime.substring(11, 16) : '16:30';

                      onEditTaskDetails({
                        id: undefined,
                        title: parsedTask.title,
                        date: datePart,
                        startTime: startH,
                        endTime: endH,
                        priority: parsedTask.priority,
                        categoryType: parsedTask.categoryType,
                        estimatedDurationMinutes: parsedTask.estimatedDurationMinutes,
                      });
                      setParsedTask(null);
                      setInputText('');
                    }}
                    style={{
                      textAlign: 'center',
                      color: '#6C5CE7',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      padding: '6px 0',
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                    }}
                  >
                    Chỉnh sửa chi tiết trước khi lưu
                  </button>
                </div>
              ) : null}
            </div>
          )}
        </div>
        )
      )}

      {/* ── Suggestions Section (Bạn cũng có thể thử) ── */}
      <div style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ fontSize: '0.82rem', color: '#64748B', fontWeight: 500 }}>
          Bạn cũng có thể thử
        </div>

        <button
          onClick={() => {
            const prompt = 'Sắp xếp cho tôi 2 tiếng đọc sách vào sáng mai lúc 8h';
            setInputText(prompt);
            executeAiParse(prompt);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '12px 14px',
            background: '#F8FAFC',
            border: '1px solid #E2E8F0',
            borderRadius: 10,
            textAlign: 'left',
            color: '#334155',
            fontSize: '0.83rem',
            lineHeight: 1.4,
            cursor: 'pointer',
            transition: 'all 0.15s',
          }}
        >
          <BookOpen size={16} style={{ color: '#6C5CE7', flexShrink: 0 }} />
          <span style={{ flex: 1 }}>Sắp xếp cho tôi 2 tiếng đọc sách vào sáng mai lúc 8h</span>
          <ExternalLink size={14} style={{ color: '#94A3B8', flexShrink: 0 }} />
        </button>

        <button
          onClick={() => {
            const prompt = 'Nhắc tôi chạy bộ 45 phút vào 6h sáng thứ 2 tuần sau';
            setInputText(prompt);
            executeAiParse(prompt);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '12px 14px',
            background: '#F8FAFC',
            border: '1px solid #E2E8F0',
            borderRadius: 10,
            textAlign: 'left',
            color: '#334155',
            fontSize: '0.83rem',
            lineHeight: 1.4,
            cursor: 'pointer',
            transition: 'all 0.15s',
          }}
        >
          <Sparkles size={16} style={{ color: '#10B981', flexShrink: 0 }} />
          <span style={{ flex: 1 }}>Nhắc tôi chạy bộ 45 phút vào 6h sáng thứ 2 tuần sau</span>
          <ExternalLink size={14} style={{ color: '#94A3B8', flexShrink: 0 }} />
        </button>

        <button
          onClick={() => {
            const prompt = 'Hãy lặp cho tôi một lịch Tiếng Anh 2 vào lúc 1h đến 2h30p khoảng 1tiếng rưỡi chiều ngày hôm sau thứ 7';
            setInputText(prompt);
            executeAiParse(prompt);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '12px 14px',
            background: '#F8FAFC',
            border: '1px solid #E2E8F0',
            borderRadius: 10,
            textAlign: 'left',
            color: '#334155',
            fontSize: '0.83rem',
            lineHeight: 1.4,
            cursor: 'pointer',
            transition: 'all 0.15s',
          }}
        >
          <BookOpen size={16} style={{ color: '#7C3AED', flexShrink: 0 }} />
          <span style={{ flex: 1 }}>Lịch Tiếng Anh 1h đến 2h30 chiều (1 tiếng rưỡi)</span>
          <ExternalLink size={14} style={{ color: '#94A3B8', flexShrink: 0 }} />
        </button>
      </div>

      {/* ── Footer Disclaimer ── */}
      <div
        style={{
          marginTop: 'auto',
          padding: '14px 20px',
          borderTop: '1px solid #F1F5F9',
          fontSize: '0.72rem',
          color: '#94A3B8',
          lineHeight: 1.5,
        }}
      >
        Mọi thay đổi đều cần bạn xác nhận. Nhập giọng nói được hỗ trợ trực tiếp qua Web Speech API tiếng Việt.
      </div>
    </aside>
  );
}
