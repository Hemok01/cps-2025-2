# UI 디자인 시스템

MobileGPT 프로젝트의 통합 UI 디자인 시스템 가이드입니다. Android 학생 앱과 웹 강사 대시보드 간 일관된 사용자 경험을 제공하기 위한 디자인 원칙과 규격을 정의합니다.

## 목차
1. [디자인 원칙](#디자인-원칙)
2. [색상 시스템](#색상-시스템)
3. [타이포그래피](#타이포그래피)
4. [간격 시스템](#간격-시스템)
5. [컴포넌트 스타일](#컴포넌트-스타일)
6. [플랫폼별 구현](#플랫폼별-구현)

---

## 디자인 원칙

### 1. 시니어 친화적 디자인
- **큰 폰트 크기**: 최소 16sp/px 이상 사용
- **고대비 색상**: WCAG AA 이상 준수
- **명확한 액션**: 버튼은 명확하고 크게 (최소 48dp/px)
- **단순한 레이아웃**: 한 화면에 하나의 주요 작업

### 2. Material Design 3 기반
- Android: Material Design 3 (Material You)
- Web: Material-UI (MUI) v7 with Material Design 3 principles

### 3. 일관성
- 양쪽 플랫폼에서 동일한 색상 팔레트 사용
- 유사한 인터랙션 패턴 유지
- 공통 아이콘 세트 (Material Icons)

---

## 색상 시스템

### Primary Colors (주요 색상)
```
Primary:     #1976D2  (파란색)
Primary Light: #42A5F5
Primary Dark:  #1565C0

용도: 주요 액션 버튼, 헤더, 강조 요소
```

### Secondary Colors (보조 색상)
```
Secondary:     #DC004E  (분홍색)
Secondary Light: #F50057
Secondary Dark:  #C51162

용도: 부차적인 액션, 강조 포인트
```

### Semantic Colors (의미 색상)

#### Success (성공)
```
Success:       #4CAF50  (초록색)
Success Light:  #81C784
Success Dark:   #388E3C

용도: 완료 상태, 성공 메시지, 진행 완료
```

#### Error (오류)
```
Error:         #F44336  (빨간색)
Error Light:    #E57373
Error Dark:     #D32F2F

용도: 에러 메시지, 경고, 삭제 액션
```

#### Warning (경고)
```
Warning:       #FF9800  (주황색)
Warning Light:  #FFB74D
Warning Dark:   #F57C00

용도: 주의 메시지, 도움 요청 상태
```

#### Info (정보)
```
Info:          #2196F3  (밝은 파란색)
Info Light:     #64B5F6
Info Dark:      #1976D2

용도: 정보 메시지, 팁
```

### Neutral Colors (중립 색상)
```
Background:    #FFFFFF  (흰색)
Surface:       #F5F5F5  (연한 회색)
Border:        #E0E0E0  (테두리 회색)

Text Primary:   #212121  (거의 검정)
Text Secondary: #757575  (중간 회색)
Text Disabled:  #BDBDBD  (연한 회색)
```

### Status Colors (상태 색상)
```
Active:        #4CAF50  (활성 - 초록)
Inactive:      #9E9E9E  (비활성 - 회색)
Pending:       #FF9800  (대기 중 - 주황)
Completed:     #1976D2  (완료 - 파란색)
Help Needed:   #F44336  (도움 필요 - 빨간색)
```

### Color Contrast Ratios (대비율)
- 본문 텍스트: 최소 4.5:1
- 큰 텍스트 (18sp+ 또는 bold 14sp+): 최소 3:1
- UI 컴포넌트: 최소 3:1

---

## 타이포그래피

### Font Families
```
Android: Roboto (시스템 기본)
Web:     Roboto, "Helvetica Neue", Arial, sans-serif
```

### Type Scale (크기 체계)

#### Headings (제목)
```
H1: 32sp/px, Weight 700 (Bold)
    용도: 페이지 메인 타이틀

H2: 28sp/px, Weight 600 (Semi-Bold)
    용도: 섹션 타이틀

H3: 24sp/px, Weight 600 (Semi-Bold)
    용도: 카드 타이틀, 서브섹션

H4: 20sp/px, Weight 600 (Semi-Bold)
    용도: 소제목
```

#### Body Text (본문)
```
Body 1: 18sp/px, Weight 400 (Regular)
        용도: 주요 본문 텍스트 (시니어 고려 큰 크기)

Body 2: 16sp/px, Weight 400 (Regular)
        용도: 보조 본문 텍스트

Caption: 14sp/px, Weight 400 (Regular)
         용도: 설명 텍스트, 메타 정보

Small: 12sp/px, Weight 400 (Regular)
       용도: 작은 라벨 (최소 사용)
```

#### Button Text (버튼 텍스트)
```
Button Large: 18sp/px, Weight 600 (Semi-Bold)
              용도: 주요 액션 버튼

Button Medium: 16sp/px, Weight 500 (Medium)
               용도: 일반 버튼

Button Small: 14sp/px, Weight 500 (Medium)
              용도: 작은 버튼 (최소 사용)
```

### Line Height
- 본문: 1.5배 (예: 18sp → 27sp line height)
- 제목: 1.2배
- 버튼: 1.0배 (단일 라인)

### Letter Spacing
- 본문: 0.5sp/px
- 버튼: 1.25sp/px (대문자일 경우)
- 제목: 0sp/px

---

## 간격 시스템

### Spacing Scale (8dp/px 기반)
```
XS:   4dp/px   - 아주 작은 간격
SM:   8dp/px   - 작은 간격
MD:   16dp/px  - 기본 간격
LG:   24dp/px  - 큰 간격
XL:   32dp/px  - 아주 큰 간격
XXL:  48dp/px  - 섹션 간 간격
```

### Padding (내부 여백)
```
Button:         16dp/px (좌우), 12dp/px (상하)
Card:           16dp/px (모든 방향)
Container:      16dp/px (모바일), 24dp/px (태블릿/웹)
List Item:      16dp/px (좌우), 12dp/px (상하)
```

### Margin (외부 여백)
```
Section:        24dp/px (섹션 간)
Component:      16dp/px (컴포넌트 간)
Element:        8dp/px (작은 요소 간)
```

### Border Radius (모서리 둥글기)
```
Small:   4dp/px   - 작은 컴포넌트 (Chip, Tag)
Medium:  8dp/px   - 기본 컴포넌트 (Button, TextField)
Large:   12dp/px  - 카드, 다이얼로그
Extra:   16dp/px  - 큰 컨테이너
Circle:  50%      - 원형 버튼, 아바타
```

### Elevation (그림자)
```
Level 0: 없음        - 배경
Level 1: 2dp/px      - 카드, 버튼
Level 2: 4dp/px      - FAB (Floating Action Button)
Level 3: 8dp/px      - 드롭다운, 모달
Level 4: 16dp/px     - 다이얼로그
```

---

## 컴포넌트 스타일

### Buttons (버튼)

#### Primary Button (주요 버튼)
```
스타일:
- Background: Primary Color (#1976D2)
- Text: White (#FFFFFF)
- Border Radius: 8dp/px
- Padding: 16dp/px (좌우), 12dp/px (상하)
- Min Height: 48dp/px
- Font: 16sp/px, Semi-Bold
- Elevation: 2dp/px

상태:
- Hover: Primary Light (#42A5F5)
- Active: Primary Dark (#1565C0)
- Disabled: #BDBDBD
```

#### Secondary Button (보조 버튼)
```
스타일:
- Background: Transparent
- Text: Primary Color (#1976D2)
- Border: 2px solid Primary Color
- Border Radius: 8dp/px
- Padding: 16dp/px (좌우), 12dp/px (상하)
- Min Height: 48dp/px
- Font: 16sp/px, Medium

상태:
- Hover: Background #E3F2FD (연한 파란색)
- Active: Primary Color (#1976D2) with opacity 0.1
- Disabled: Border #BDBDBD, Text #BDBDBD
```

#### Text Button (텍스트 버튼)
```
스타일:
- Background: Transparent
- Text: Primary Color (#1976D2)
- No Border
- Padding: 8dp/px (좌우), 4dp/px (상하)
- Min Height: 36dp/px
- Font: 16sp/px, Medium

상태:
- Hover: Background #E3F2FD (연한 파란색)
- Active: Primary Color with opacity 0.1
```

#### Danger Button (위험 버튼)
```
스타일:
- Background: Error Color (#F44336)
- Text: White (#FFFFFF)
- Border Radius: 8dp/px
- 나머지는 Primary Button과 동일

용도: 삭제, 종료 등 중요한 액션
```

### Cards (카드)

```
스타일:
- Background: White (#FFFFFF)
- Border: 1px solid #E0E0E0
- Border Radius: 12dp/px
- Padding: 16dp/px
- Elevation: 1dp/px

상태:
- Hover: Elevation 2dp/px
- Active: Border Primary Color (#1976D2)

구성:
- Card Header: H3 title + optional subtitle
- Card Content: Body text
- Card Actions: 버튼들 (우측 정렬)
```

### Input Fields (입력 필드)

#### Text Field
```
스타일:
- Border: 1px solid #E0E0E0
- Border Radius: 8dp/px
- Padding: 12dp/px (좌우), 14dp/px (상하)
- Min Height: 48dp/px
- Font: 16sp/px, Regular
- Background: White (#FFFFFF)

상태:
- Focus: Border 2px solid Primary (#1976D2)
- Error: Border 2px solid Error (#F44336)
- Disabled: Background #F5F5F5, Text #BDBDBD

라벨:
- 위치: 상단 (Floating Label) 또는 고정
- Font: 14sp/px, Medium
- Color: Text Secondary (#757575)
```

#### Select / Dropdown
```
스타일:
- TextField와 동일
- 우측에 아이콘 (expand_more)
- Dropdown Menu:
  - Background: White
  - Border Radius: 8dp/px
  - Elevation: 3dp/px
  - Item Padding: 12dp/px (좌우), 10dp/px (상하)
  - Item Hover: Background #F5F5F5
```

### Chips / Tags

```
스타일:
- Background: #E0E0E0 (중립) 또는 색상별
- Text: Text Primary (#212121)
- Border Radius: 16dp/px
- Padding: 8dp/px (좌우), 4dp/px (상하)
- Font: 14sp/px, Medium
- Height: 32dp/px

상태별 색상:
- Active: Success (#4CAF50), White text
- Pending: Warning (#FF9800), White text
- Error: Error (#F44336), White text
- Info: Info (#2196F3), White text
```

### Alerts / Notifications

```
스타일:
- Background: 색상별 Light variant
- Border-Left: 4px solid 색상별 Main
- Border Radius: 8dp/px
- Padding: 12dp/px (좌우), 10dp/px (상하)
- Icon: 좌측에 색상별 아이콘

타입별:
- Success: Background #E8F5E9, Border #4CAF50, Icon check_circle
- Error: Background #FFEBEE, Border #F44336, Icon error
- Warning: Background #FFF3E0, Border #FF9800, Icon warning
- Info: Background #E3F2FD, Border #2196F3, Icon info
```

### Tables (테이블)

```
스타일:
- Header:
  - Background: #F5F5F5
  - Text: Text Primary, Semi-Bold
  - Border-Bottom: 2px solid #E0E0E0
  - Padding: 12dp/px (좌우), 10dp/px (상하)

- Row:
  - Background: White
  - Border-Bottom: 1px solid #E0E0E0
  - Padding: 12dp/px (좌우), 10dp/px (상하)
  - Hover: Background #F5F5F5

- Cell Alignment:
  - Text: Left
  - Numbers: Right
  - Actions: Center
```

### Progress Indicators (진행 표시)

#### Linear Progress Bar
```
스타일:
- Height: 8dp/px
- Border Radius: 4dp/px
- Background: #E0E0E0
- Fill: Primary Color (#1976D2)
- Animated: 진행 중일 때 애니메이션

상태별 색상:
- Success: #4CAF50
- Warning: #FF9800
- Error: #F44336
```

#### Circular Progress
```
스타일:
- Size: 40dp/px (기본)
- Stroke Width: 4dp/px
- Color: Primary (#1976D2)
- Indeterminate: 회전 애니메이션
```

### Dialogs / Modals

```
스타일:
- Background: White (#FFFFFF)
- Border Radius: 12dp/px
- Elevation: 4dp/px
- Max Width: 600px (웹)
- Padding: 24dp/px

구성:
- Title: H3, 24sp/px, Semi-Bold
- Content: Body 1, 18sp/px
- Actions: 버튼들 (우측 정렬, 8dp 간격)

오버레이:
- Background: Black with opacity 0.5
```

---

## 플랫폼별 구현

### Android (Kotlin Compose)

#### Color Theme
```kotlin
// app/src/main/java/com/mobilegpt/student/ui/theme/Color.kt
val PrimaryColor = Color(0xFF1976D2)
val PrimaryLightColor = Color(0xFF42A5F5)
val PrimaryDarkColor = Color(0xFF1565C0)

val SecondaryColor = Color(0xFFDC004E)
val ErrorColor = Color(0xFFF44336)
val SuccessColor = Color(0xFF4CAF50)
val WarningColor = Color(0xFFFF9800)

val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)
val BackgroundColor = Color(0xFFFFFFFF)
val SurfaceColor = Color(0xFFF5F5F5)
```

#### Typography
```kotlin
// app/src/main/java/com/mobilegpt/student/ui/theme/Type.kt
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
)
```

#### Button Example
```kotlin
Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryColor,
        contentColor = Color.White
    ),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 2.dp
    )
) {
    Text(
        text = "버튼 텍스트",
        style = MaterialTheme.typography.labelLarge
    )
}
```

#### Card Example
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color.White
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp
    ),
    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Card content
    }
}
```

---

### Web (React + MUI)

#### Theme Configuration
```typescript
// frontend-teacher/src/theme.ts
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976D2',
      light: '#42A5F5',
      dark: '#1565C0',
    },
    secondary: {
      main: '#DC004E',
      light: '#F50057',
      dark: '#C51162',
    },
    success: {
      main: '#4CAF50',
      light: '#81C784',
      dark: '#388E3C',
    },
    error: {
      main: '#F44336',
      light: '#E57373',
      dark: '#D32F2F',
    },
    warning: {
      main: '#FF9800',
      light: '#FFB74D',
      dark: '#F57C00',
    },
    info: {
      main: '#2196F3',
      light: '#64B5F6',
      dark: '#1976D2',
    },
    text: {
      primary: '#212121',
      secondary: '#757575',
      disabled: '#BDBDBD',
    },
    background: {
      default: '#FFFFFF',
      paper: '#F5F5F5',
    },
  },
  typography: {
    fontFamily: 'Roboto, "Helvetica Neue", Arial, sans-serif',
    h1: {
      fontSize: '32px',
      fontWeight: 700,
    },
    h2: {
      fontSize: '28px',
      fontWeight: 600,
    },
    h3: {
      fontSize: '24px',
      fontWeight: 600,
    },
    body1: {
      fontSize: '18px',
      lineHeight: 1.5,
    },
    body2: {
      fontSize: '16px',
      lineHeight: 1.5,
    },
    button: {
      fontSize: '16px',
      fontWeight: 600,
      textTransform: 'none',
    },
  },
  spacing: 8,
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: '48px',
          borderRadius: '8px',
          padding: '12px 16px',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: '12px',
          border: '1px solid #E0E0E0',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: '8px',
            minHeight: '48px',
          },
        },
      },
    },
  },
});
```

#### Button Example
```tsx
import { Button } from '@mui/material';

// Primary Button
<Button
  variant="contained"
  color="primary"
  onClick={handleClick}
  sx={{ minHeight: 48, px: 2 }}
>
  버튼 텍스트
</Button>

// Secondary Button
<Button
  variant="outlined"
  color="primary"
  onClick={handleClick}
>
  버튼 텍스트
</Button>

// Danger Button
<Button
  variant="contained"
  color="error"
  onClick={handleDelete}
>
  삭제
</Button>
```

#### Card Example
```tsx
import { Card, CardContent, CardActions, Typography } from '@mui/material';

<Card sx={{ borderRadius: 3, border: '1px solid #E0E0E0' }}>
  <CardContent>
    <Typography variant="h3" gutterBottom>
      카드 제목
    </Typography>
    <Typography variant="body1">
      카드 내용
    </Typography>
  </CardContent>
  <CardActions sx={{ justifyContent: 'flex-end', p: 2 }}>
    <Button>취소</Button>
    <Button variant="contained">확인</Button>
  </CardActions>
</Card>
```

#### Alert Example
```tsx
import { Alert } from '@mui/material';

<Alert severity="success" sx={{ mb: 2 }}>
  성공 메시지
</Alert>

<Alert severity="error">
  에러 메시지
</Alert>

<Alert severity="warning">
  경고 메시지
</Alert>

<Alert severity="info">
  정보 메시지
</Alert>
```

---

## 접근성 고려사항

### 1. Color Contrast (색상 대비)
- 모든 텍스트는 WCAG AA 이상 준수
- 본문: 최소 4.5:1
- 큰 텍스트: 최소 3:1

### 2. Touch Targets (터치 영역)
- 최소 크기: 48dp/px × 48dp/px
- 버튼 간 최소 간격: 8dp/px

### 3. Font Size (폰트 크기)
- 최소 본문 크기: 16sp/px (시니어 고려)
- 주요 액션 버튼: 18sp/px 이상

### 4. Focus Indicators (포커스 표시)
- 키보드 네비게이션 시 명확한 포커스 표시
- Focus Outline: 2px solid Primary Color

### 5. Screen Readers (스크린 리더)
- 모든 인터랙티브 요소에 적절한 label 제공
- Android: contentDescription
- Web: aria-label, aria-describedby

---

## 애니메이션 가이드

### Duration (지속 시간)
```
Short:  150ms  - 간단한 상태 변화 (hover, active)
Medium: 300ms  - 일반 전환 (페이지, 다이얼로그)
Long:   500ms  - 복잡한 애니메이션
```

### Easing (가속도)
```
Standard:    cubic-bezier(0.4, 0.0, 0.2, 1)  - 일반
Decelerate:  cubic-bezier(0.0, 0.0, 0.2, 1)  - 진입
Accelerate:  cubic-bezier(0.4, 0.0, 1, 1)    - 이탈
```

### 사용 제한
- 과도한 애니메이션 지양 (시니어 사용자 고려)
- 필수 피드백에만 사용
- 사용자가 제어 가능해야 함

---

## 다크 모드 (선택사항)

현재 프로젝트는 라이트 모드만 지원하지만, 향후 다크 모드 지원 시 다음 색상 팔레트 사용:

```
Background:     #121212
Surface:        #1E1E1E
Primary:        #90CAF9  (밝은 파란색)
Text Primary:   #FFFFFF
Text Secondary: #B0B0B0
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0  | 2025-11-14 | 초안 작성 |

---

## 참고 자료

- [Material Design 3](https://m3.material.io/)
- [Material-UI (MUI) Documentation](https://mui.com/)
- [Jetpack Compose Material Design](https://developer.android.com/jetpack/compose/designsystems/material3)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
