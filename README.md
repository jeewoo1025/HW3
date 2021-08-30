# Android App of Music Player

<b>멜론, 지니 같은 뮤직 플레이어 앱 클론 프로젝트 (Clone Proejct)</b> <br>
MediaPlayer, ForegroundService를 사용하여 멀티미디어 기능 구현함
<br>

## 실행화면 (MainActivity)
ListView, MediaStore 클래스를 사용하면 화면, 기능 구현

![img](https://user-images.githubusercontent.com/39071676/131359705-f62010ba-c01c-474c-8bcc-9b7d89d07194.png)
<br>

## 음악 재생화면 (PlayMusicActivity)
- 타이틀 이미지, 제목, 이전/다음 버튼, 재생 및 일시정지 버튼, ProgressBar로 구성
- BroadCast Receiver를 사용

![image](https://user-images.githubusercontent.com/39071676/131360886-ddd03cfe-535e-4458-8a73-ab093083e00b.png)
<br>

## 음악 재생시 홈 화면에서의 처리 (상단바 아이콘, 스크롤바)
- 상단바 아이콘
  - 홈 버튼이 눌렀을 때에도 음악이 재생중이면 재생 아이콘, 일시정지 상황이면 일시정지 아이콘을 보여줌

- 스크롤 바를 내렸을 때 현재 재생되는 음악의 정보가 나와야한다
  - RemoteViews 사용

![image](https://user-images.githubusercontent.com/39071676/131361043-2f6aca23-c959-401f-b677-40ca68e431c5.png)  
