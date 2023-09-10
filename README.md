## TDatabase
![MC](https://img.shields.io/badge/Minecraft-1.19-62B47A?style=flat-square)
![SQLite](https://img.shields.io/badge/SQLite-3-003B57?style=flat-square&logo=sqlite)
![JDK](https://img.shields.io/badge/JDK-17-F80000?style=flat-square&logo=Oracle)
![Project](https://img.shields.io/badge/TDatabase-1.1_SNAPSHOT-black?style=flat-square)

마인크래프트 플러그인을 개발할 때 손쉽게 데이터베이스를 사용할 수 있게 하기 위한 API 플러그인입니다.

SQLite를 사용하여 별도의 프로그램 설치가 필요하지 않고 플러그인 별 데이터베이스 파일을 만들어 관리하고

타 플러그인에서 관리하는 객체에 맞추어 데이터베이스를 자동으로 구성하고 매핑하는 기능을 제공합니다.

---

### Import

* #### Gradle


    Preparing

* #### Maven


    Preparing

* #### Local


    Preparing

---

### Usage

타 플러그인에서 사용할 메소드는 TDatabase 클래스에 모두 정적 선언되어 있습니다.

#### 1. Entity 클래스 작성

관리할 클래스에 어노테이션을 작성하여야 데이터베이스 테이블을 자동으로 생성할 수 있습니다.
각 변수는 Column 값으로 사용되며 변수들에게는 특정 옵션을 부여할 수 있습니다.

* @Table : TDatabase에서 관리할 클래스에 작성합니다.
* @PrimaryKey : 테이블에서 Primary Key로 사용할 컬럼 변수에 작성합니다.
* @Column : 작성하지 않아도 무관하며 기본적으로 nullable은 참으로 인식합니다.

**가급적 변수의 타입은 참조형(reference type)으로 작성을 권장합니다.**

```java
@Table
public class PlayerStat{
    
    ...Constructor...
    
    @PrimaryKey
    private String uuid;
    
    @Column(nullable = false)
    private Integer strength;
    
    @Column(nullable = false)
    private Integer healthPoint;
    
    @Column(nullable = false)
    private Integer manaPoint;
}
```

#### 2. 초기화

클래스를 TDatabase에 입력하여 데이터베이스 연결을 초기화하여야 데이터베이스 및 테이블이 생성되어 사용이 가능합니다.

만약 Entity 클래스를 상기 기술된 1번 형식에 맞추지 않을 경우 InitializeDatabaseException 이 발생합니다.

**가급적 플러그인 활성화 시점(onEnable)에 초기화하는 것을 권장합니다.**

```java
TDatabase.initializeDatabase(plugin, PlayerStat.class);
//클래스 매개변수는 가변인자로도 입력할 수 있습니다.
TDatabase.initializeDatabase(plugin, PlayerStat.class, Member.class);
```

#### 3. 데이터 저장

클래스를 TDatabase에 입력하여 데이터베이스 연결이 완료되었다면 해당 클래스의 객체를 저장할 수 있습니다.

```java
//Example
PlayerStat playerStat = new PlayerStat(player.getUniqueId(), 5, 20, 20);

TDatabase.saveData(plugin, playerStat);
```

#### 4. Primary Key 컬럼의 데이터 찾기

객체를 데이터베이스에서 가져오기 위해서는 Primary Key 값을 사용하여 가져와야합니다.

Primary Key를 알고 있다면 ***5. 특정 레코드를 찾아 객체에 매핑***으로 넘어가 사용해도 무관합니다.

테이블에 저장되어있는 레코드들의 Primary Key 모두를 문자열 배열로 반환받아 사용할 수 있습니다.

```java
String[] primaryKeys = TDatabase.getPrimaryKeysFromDatabase(plugin, PlayerStat.class);
```

#### 5. 특정 레코드를 찾아 객체에 매핑

Primary Key를 알고 있다면 그 값을 이용해 레코드를 찾고 객체에 매핑시켜 반환받아 사용할 수 있습니다.

```java
PlayerStat stat = TDatabase.getDataFromDatabase(plugin, primaryKey, PlayerStat.class);
```

#### 6. 모든 레코드를 객체에 매핑하여 리스트 얻기

만약 테이블에 저장된 모든 레코드를 한번에 객체에 매핑받아 사용해야할 수도 있습니다.

```java
List<PlayerStat> statList = TDatabase.getAllInstance(plugin, Playerstat.class);
```

#### 7. 데이터베이스 연결 임의로 종료하기

SQLite는 데이터베이스가 한 프로세스에 연결되어있으면 수정하지 못하도록 파일이 잠깁니다.

따라서 개발자가 임의로 데이터베이스 연결을 종료할 수 있어야 합니다.

모든 경우에 개발자가 수동으로 닫아야 하는 것은 아닙니다. 

해당 플러그인이 비활성화(onDisable)되는 시점에 자동으로 모든 연결은 종료됩니다.

```java
TDatabase.closeDatabase(plugin);
```

---

### Exception

TDatabase에서는 초기화, 저장, 불러오기 기능에 대한 예외 처리 클래스를 제공합니다.

#### 1. InitializeDatabaseException / ClassValidationException

만약 초기화가 불가능한 클래스를 입력하여 초기화하려할 때 발생합니다.

상기 ***Usage의 1. Entity 클래스 작성***을 확인하여 정확히 작성하여 해결할 수 있습니다.

#### 2. NoSuchInstanceInDatabaseException

만약 찾으려는 레코드가 테이블 내에 존재하지 않을 경우 발생합니다.

#### 3. DatabaseSaveException

만약 클래스 입력을 사전에 하지 않아 데이터베이스에서 초기화하지 못한 상황일 때 저장하지 못해 발생합니다.

상기 ***Usage의 2. 초기화***를 확인하여 사전에 초기화하여 해결할 수 있습니다.

#### 4. CloseDatabaseException

연결하였으나 사용자가 임의로 연결을 종료한 데이터베이스에 접근할 때 발생합니다.
