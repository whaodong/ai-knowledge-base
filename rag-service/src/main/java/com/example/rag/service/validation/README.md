# 答案验证模块

## 概述

答案验证模块提供多层次、多维度的答案验证机制，用于检测和缓解AI幻觉，提高答案质量和可信度。

## 核心组件

### 1. FactConsistencyChecker（事实一致性检查器）

检查答案与检索文档之间的事实一致性。

**主要功能：**
- 提取答案中的关键声明（数字、时间、实体等）
- 与检索文档进行事实比对
- 检测矛盾和不一致
- 生成一致性评分

**使用示例：**
```java
@Autowired
private FactConsistencyChecker consistencyChecker;

ConsistencyCheckResult result = consistencyChecker.check(answer, retrievalResults);

// 获取一致性评分
double score = result.getConsistencyScore();

// 检查是否有矛盾
boolean hasContradictions = result.hasContradictions();

// 获取支持的声明数量
int supportedCount = result.getSupportedClaimsCount();
```

### 2. CitationValidator（引用验证器）

验证答案中的引用是否准确、可信。

**主要功能：**
- 提取答案中的各种引用格式
- 验证引用来源是否存在
- 检查引用是否准确
- 生成引用可信度评分
- 标注引用位置

**支持的引用格式：**
- 数字引用：[1], [2], (1), (2)
- 文本引用：[来源名称]
- 来源引用："根据XX"
- 链接引用：http://...

**使用示例：**
```java
@Autowired
private CitationValidator citationValidator;

CitationValidationResult result = citationValidator.validate(answer, retrievalResults);

// 获取引用评分
double citationScore = result.getCitationScore();

// 获取有效引用数量
int validCount = result.getValidCitationsCount();

// 获取未引用的重要信息
List<UnreferencedClaim> unreferenced = result.getUnreferencedClaims();
```

### 3. AnswerScorer（多维度评分服务）

综合多个维度对答案进行评分。

**评分维度：**
- **相关性（0-1）**：答案与问题的相关程度
- **一致性（0-1）**：答案与检索文档的事实一致性
- **引用评分（0-1）**：引用的准确性和可信度
- **完整性（0-1）**：答案的完整程度
- **综合置信度（0-1）**：加权平均后的总体评分

**使用示例：**
```java
@Autowired
private AnswerScorer answerScorer;

AnswerScore score = answerScorer.score(answer, query, retrievalResults);

// 获取综合置信度
double confidence = score.getConfidence();

// 判断是否为高质量答案
boolean isHighQuality = score.isHighQuality();

// 获取需要改进的维度
Map<String, Double> weakDimensions = score.getWeakDimensions();
```

### 4. AnswerCorrector（答案修正服务）

对低置信度答案进行修正和优化。

**主要功能：**
- 低置信度答案标记
- 自动添加免责声明
- 建议用户核实
- 提供原始来源链接

**使用示例：**
```java
@Autowired
private AnswerCorrector answerCorrector;

CorrectedAnswer corrected = answerCorrector.correct(answer, score, retrievalResults);

// 获取修正后的答案
String correctedText = corrected.getCorrectedText();

// 检查是否被修正
boolean wasCorrected = corrected.isWasCorrected();

// 获取修正项列表
List<Correction> corrections = corrected.getCorrections();
```

### 5. HallucinationDetector（幻觉检测器）

检测AI生成的答案中可能存在的幻觉内容。

**检测类型：**
- 无依据的陈述
- 过度推断
- 数字/时间不一致
- 虚构内容
- 虚构实体

**使用示例：**
```java
@Autowired
private HallucinationDetector hallucinationDetector;

HallucinationDetectionResult result = hallucinationDetector.detect(answer, retrievalResults);

// 获取幻觉评分（越高越好，表示幻觉越少）
double score = result.getHallucinationScore();

// 检查是否检测到幻觉
boolean hasHallucinations = result.hasHallucinations();

// 获取高严重性幻觉数量
int highSeverityCount = result.getHighSeverityCount();
```

### 6. ValidationReportGenerator（验证报告生成器）

生成详细的答案验证报告。

**使用示例：**
```java
@Autowired
private ValidationReportGenerator reportGenerator;

ValidationReport report = reportGenerator.generateReport(
    answer, query, score, consistencyResult, 
    citationResult, hallucinationResult, retrievalResults
);

// 生成Markdown格式报告
String markdownReport = reportGenerator.generateMarkdownReport(report);
```

## 统一验证接口

### AnswerValidationService

集成所有验证组件，提供统一的答案验证接口。

**完整验证：**
```java
@Autowired
private AnswerValidationService validationService;

ValidationResult result = validationService.validate(answer, query, retrievalResults);

// 获取最终答案（可能已被修正）
String finalAnswer = result.getFinalAnswer();

// 获取评分
AnswerScore score = result.getScore();

// 获取验证报告
ValidationReport report = result.getReport();

// 判断是否通过验证
boolean passed = result.isPassed();

// 获取Markdown格式报告
String markdownReport = result.getMarkdownReport();
```

**快速评分：**
```java
AnswerScore score = validationService.quickScore(answer, query, retrievalResults);
```

**单项验证：**
```java
// 仅验证一致性
ConsistencyCheckResult consistencyResult = validationService.validateConsistency(answer, retrievalResults);

// 仅验证引用
CitationValidationResult citationResult = validationService.validateCitations(answer, retrievalResults);

// 仅检测幻觉
HallucinationDetectionResult hallucinationResult = validationService.detectHallucinations(answer, retrievalResults);
```

## REST API

### 1. 完整验证接口

**POST** `/api/validation/validate`

**请求体：**
```json
{
  "answer": "生成的答案文本...",
  "query": "用户问题",
  "retrievalResults": [
    {
      "documentId": "doc-001",
      "content": "检索到的文档内容...",
      "metadata": {
        "source": "来源名称",
        "timestamp": "2024-01-01"
      },
      "rawScore": 0.85,
      "rerankScore": 0.90
    }
  ]
}
```

**响应：**
```json
{
  "originalAnswer": "原始答案",
  "finalAnswer": "最终答案（可能已修正）",
  "wasCorrected": true,
  "passed": true,
  "score": {
    "relevance": 0.85,
    "consistency": 0.90,
    "citationScore": 0.80,
    "completeness": 0.85,
    "hallucinationScore": 0.95,
    "confidence": 0.87,
    "level": "GOOD"
  },
  "consistencyScore": 0.90,
  "citationScore": 0.80,
  "hallucinationScore": 0.95,
  "totalValidationTimeMs": 150
}
```

### 2. 快速评分接口

**POST** `/api/validation/score`

### 3. 一致性检查接口

**POST** `/api/validation/consistency`

### 4. 引用验证接口

**POST** `/api/validation/citations`

### 5. 幻觉检测接口

**POST** `/api/validation/hallucinations`

### 6. 验证报告接口

**POST** `/api/validation/report`

### 7. 答案修正接口

**POST** `/api/validation/correct`

## 配置说明

在 `application.yml` 中配置：

```yaml
rag:
  validation:
    # 是否启用验证功能
    enabled: true
    # 是否自动修正低质量答案
    auto-correct: true
    # 置信度阈值（低于此值将触发修正）
    confidence-threshold: 0.7
  
  # 评分配置
  scoring:
    # 各维度权重配置
    relevance-weight: 0.25
    consistency-weight: 0.30
    citation-weight: 0.15
    completeness-weight: 0.15
    hallucination-weight: 0.15
  
  # 答案修正配置
  correction:
    confidence-threshold: 0.7
    enable-disclaimer: true
    enable-suggestions: true
    enable-sources: true
```

## 验证流程

```
1. 接收答案和检索结果
   ↓
2. 事实一致性检查
   - 提取关键声明
   - 与检索文档比对
   - 识别矛盾点
   ↓
3. 引用验证
   - 提取引用
   - 验证来源
   - 计算可信度
   ↓
4. 幻觉检测
   - 检测无依据陈述
   - 识别过度推断
   - 发现不一致
   ↓
5. 多维度评分
   - 计算各维度分数
   - 加权平均得到综合置信度
   ↓
6. 答案修正（如需要）
   - 添加免责声明
   - 标记问题
   - 提供来源
   ↓
7. 生成验证报告
   - 汇总所有验证结果
   - 提供改进建议
```

## 最佳实践

### 1. 合理设置置信度阈值

```yaml
rag:
  validation:
    confidence-threshold: 0.7  # 根据业务需求调整
```

- 0.8以上：高质量答案，直接使用
- 0.7-0.8：可接受质量，建议核实关键信息
- 0.6-0.7：质量较低，需要人工审核
- 0.6以下：不可接受，建议重新生成

### 2. 根据场景调整权重

```yaml
rag:
  scoring:
    # 学术场景：提高一致性和引用权重
    consistency-weight: 0.35
    citation-weight: 0.25
    
    # 客服场景：提高相关性和完整性权重
    relevance-weight: 0.35
    completeness-weight: 0.25
```

### 3. 集成到RAG流程

```java
@Service
public class EnhancedRagService {
    
    @Autowired
    private RagRetrievalService retrievalService;
    
    @Autowired
    private AnswerValidationService validationService;
    
    public EnhancedRagResponse query(String query) {
        // 1. 检索
        RagResponse retrievalResponse = retrievalService.retrieve(request);
        
        // 2. 生成答案（调用LLM）
        String answer = generateAnswer(query, retrievalResponse.getFusedContext());
        
        // 3. 验证答案
        ValidationResult validation = validationService.validate(
            answer, query, retrievalResponse.getRetrievedDocuments()
        );
        
        // 4. 返回增强响应
        return EnhancedRagResponse.builder()
            .answer(validation.getFinalAnswer())
            .score(validation.getScore())
            .wasCorrected(validation.isWasCorrected())
            .report(validation.getReport())
            .build();
    }
}
```

## 监控和调试

### 1. 日志配置

```yaml
logging:
  level:
    com.example.rag.service.validation: DEBUG
```

### 2. 性能监控

验证服务会记录各阶段的耗时：
- consistencyCheckTimeMs: 一致性检查耗时
- citationValidationTimeMs: 引用验证耗时
- hallucinationDetectionTimeMs: 幻觉检测耗时
- scoringTimeMs: 评分耗时
- totalValidationTimeMs: 总耗时

### 3. 验证报告

通过 `/api/validation/report` 接口获取详细的Markdown格式报告，便于分析和调试。

## 注意事项

1. **性能考虑**：完整验证会增加处理时间，可根据场景选择快速评分模式
2. **阈值调优**：建议根据实际业务场景和数据质量调整置信度阈值
3. **来源质量**：验证效果依赖于检索结果的质量，确保检索系统优化良好
4. **误报处理**：某些场景可能出现误报，建议结合人工审核机制
