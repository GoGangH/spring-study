# JPA & Hibernate

## JPA?
>JPA는 ORM데이터 접근 기술을 제공하는 인터페이스 개념이다.
> JPA의 구현체는 다양하며 한가지 예시로 Hibernate가 있다.

## JPA Schema 자동생성 기능
jpa.generate 설정은 JPA 구현체 DDL(Data Definition Language)생성 옵션의 링크이고, true / false 밖에 선택할 수 없다.<br/>
ddl-auto같은 기능동작은 Hibernate에서 동작한다.

다음 목록중 하나를 `spring.jpa.hibernate.ddl-auto` 옵션의 값으로 지정할 수 있다.

- `none`: 아무것도 실행하지 않는다 (대부분의 DB에서 기본값이다)
- `create-drop`: SessionFactory가 시작될 때 drop및 생성을 실행하고, SessionFactory가 종료될 때 drop을 실행한다 (in-memory DB의 경우 기본값이다)
- `create`: SessionFactory가 시작될 때 데이터베이스 drop을 실행하고 생성된 DDL을 실행한다
- `update`: 변경된 스키마를 적용한다
- `validate`: 변경된 스키마가 있다면 변경점을 출력하고 애플리케이션을 종료한다

>ddl auto를 이용하여 업데이트를 통해 스키마 변경이 가능하나 삭제의 경우 실수했을시 큰 문제를 가져오기때문에 자동으로 반영이 불가능하다.

칼럼 추가는 가능, 칼럼의 내용(type...)의 변경은 적용되지 않는다.

## UpdateSchema 동작 code 분석

## 명세한 문서를 이용해서 table생성

HibernateSchemaManagementTool.java

### 변수 및 객체에 대한 사전설명
- `Metadata metadata` : 제공된 매핑 소스를 집계하여 결정된 ORM 모델을 나타냅니다.
- Metadata :
```text
        uuid,
        options,
        entityBindingMap,
        composites,
        mappedSuperClasses,
        collectionBindingMap,
        typeDefRegistry.copyRegistrationMap(),
        filterDefinitionMap,
        fetchProfileMap,
        imports,
        idGeneratorDefinitionMap,
        namedQueryMap,
        namedNativeQueryMap,
        namedProcedureCallMap,
        sqlResultSetMappingMap,
        namedEntityGraphMap,
        sqlFunctionMap,
        getDatabase(),
        bootstrapContext
```
- `ServiceRegistry serviceRegistry` :
- `Map<String,Object> configurationValues` : 구성 값
- `DelayedDropRegistry delayedDropRegistry` :  빌드된 DelayedDropAction을 SessionFactory(또는 이후 실행을 관리할 항목)에 다시 등록할 수 있도록 하는 콜백입니다.

```java
//DefaultSessionFactoryBuilderService.java
public SessionFactoryBuilderImplementor createSessionFactoryBuilder(final MetadataImpl metadata, final BootstrapContext bootstrapContext) {
		return new SessionFactoryBuilderImpl( metadata, bootstrapContext );
	}
//
```

sessionfactory를 빌드하는 과정에서 Schema 생성
```java
SchemaManagementToolCoordinator.process(
					bootMetamodel, //metadata
					serviceRegistry, //서비스를 할 레지스트리
					properties, //데이터베이스 구성 속성
					action -> SessionFactoryImpl.this.delayedDropAction = action //지연된 스키마 삭제를 수행합니다.
			);

//serviceRegistry
        serviceRegistry = options
        .getServiceRegistry()
        .getService( SessionFactoryServiceRegistryFactory.class )
        .buildServiceRegistry( this, options );
```

```java
public static void process(
final Metadata metadata,
final ServiceRegistry serviceRegistry,
final Map<String,Object> configurationValues, //properties
        DelayedDropRegistry delayedDropRegistry) {
// metadata에 있는 contributors에 configurationValues에 있는 action코드를 분류 database, script
final Set<ActionGrouping> groupings = ActionGrouping.interpret( metadata, configurationValues );

        //수행할 action이 없다면 리턴
        if ( groupings.isEmpty() ) {
        // no actions specified
        log.debug( "No actions found; doing nothing" );
        return;
        }
```
metadata와 configurationValues를 넣으면 action의 정보를 기반으로 그룹을 제작한다.

Action에는 아래와 같은 상수가 있다.
```java
NONE( "none" )
CREATE_ONLY( "create", "create-only" )
DROP( "drop" )
CREATE( "drop-and-create", "create" )
CREATE_DROP( null, "create-drop" )
VALIDATE( null, "validate" )
UPDATE( null, "update" )
```

Action 로직 동작은 다음과 같다.

```java
for ( ActionGrouping grouping : groupings ) {
			// for database action
			if ( grouping.databaseAction != Action.NONE ) { //Action이 None이라면 패스
				final Set<String> contributors; //중복이 없게 set설정
				if ( databaseActionMap == null ) {
					databaseActionMap = new HashMap<>();
					contributors = new HashSet<>(); 
					databaseActionMap.put( grouping.databaseAction, contributors );
				}
				else {//databaseActionMap에 값이 존재하면 중복 체크후 넣어준다.
					contributors = databaseActionMap.computeIfAbsent(
							grouping.databaseAction,
							action -> new HashSet<>()
					);
				}
				contributors.add( grouping.contributor ); //contributor를 추가한다.
			}
            
            //script도 똑같은 방법으로 저장
			// for script action
			if ( grouping.scriptAction != Action.NONE ) {
				final Set<String> contributors;
				if ( scriptActionMap == null ) {
					scriptActionMap = new HashMap<>();
					contributors = new HashSet<>();
					scriptActionMap.put( grouping.scriptAction, contributors );
				}
				else {
					contributors = scriptActionMap.computeIfAbsent(
							grouping.scriptAction,
							action -> new HashSet<>()
					);
				}
				contributors.add( grouping.contributor );
			}
		}
```

```java
final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );
final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

final boolean haltOnError = configService.getSetting(
        AvailableSettings.HBM2DDL_HALT_ON_ERROR,//가져올 설정의 이름
        StandardConverters.BOOLEAN, //적용할 변환기
        false
); //지정된 변환기와 기본값을 사용하여 명명된 설정을 가져옵니다.
final ExceptionHandler exceptionHandler = haltOnError ? ExceptionHandlerHaltImpl.INSTANCE : ExceptionHandlerLoggedImpl.INSTANCE; //에러 형식 정의 	
        //CommandAcceptanceException 오류 처리 or CommandAcceptanceException 오류 처리

final ExecutionOptions executionOptions = buildExecutionOptions( //build 실행 옵션
        configurationValues,
        exceptionHandler //오류 처리
		);
```

## Action 동작
```java
if ( databaseActionMap != null ) { //databaseAction이 존재하면
    databaseActionMap.forEach(
            (action, contributors) -> {

                performDatabaseAction( // Action에 맞는 수행
                        action,
                        metadata,
                        tool,
                        serviceRegistry,
                        executionOptions,
                        (exportable) -> contributors.contains( exportable.getContributor() )
                );

                if ( action == Action.CREATE_DROP ) { //action이 create drop일때
                    delayedDropRegistry.registerOnCloseAction( // 빌드된 DelayedDropAction 등록
                            tool.getSchemaDropper( configurationValues ).buildDelayedAction( //스키마 삭제를 수행하기 위해 지연된 Runnable을 빌드합니다. 이는 기본 데이터 저장소를 암시적으로 대상으로 합니다.
                                    metadata, //드롭할 메타데이터
                                    executionOptions, //options- 드롭 옵션
                                    (exportable) -> contributors.contains( exportable.getContributor() ), //사용할 Contributable 인스턴스에 대한 필터
                                    buildDatabaseTargetDescriptor(
                                            configurationValues,
                                            DropSettingSelector.INSTANCE,
                                            serviceRegistry
                                    )
                            )
                    );
                }
            }
    );
```

```java
//performDatabaseAction
switch ( action ){
        //update하나만을 예시로 든다.
        case UPDATE:{
            final JpaTargetAndSourceDescriptor migrateDescriptor=buildDatabaseTargetDescriptor(
                executionOptions.getConfigurationValues(),
                MigrateSettingSelector.INSTANCE,
                serviceRegistry
            );
            tool.getSchemaMigrator(executionOptions.getConfigurationValues()).doMigration(
                metadata,
                executionOptions,
                contributableInclusionFilter,
                migrateDescriptor
            );
            break;
        }
}
```

```java
private static JpaTargetAndSourceDescriptor buildDatabaseTargetDescriptor(
        Map<?,?> configurationValues,
        SettingSelector settingSelector,
        ServiceRegistry serviceRegistry) {
    final Object scriptSourceSetting = settingSelector.getScriptSourceSetting( configurationValues );
    final SourceType sourceType = SourceType.interpret(
            settingSelector.getSourceTypeSetting( configurationValues ),
            scriptSourceSetting != null ? SourceType.SCRIPT : SourceType.METADATA
    );

    final boolean includesScripts = sourceType != SourceType.METADATA;
    if ( includesScripts && scriptSourceSetting == null ) {
        throw new SchemaManagementException(
                "Schema generation configuration indicated to include CREATE scripts, but no script was specified"
        );
    }

    final ScriptSourceInput scriptSourceInput = includesScripts
            ? Helper.interpretScriptSourceSetting(
                    scriptSourceSetting,
                    serviceRegistry.getService( ClassLoaderService.class ),
                    (String) configurationValues.get( AvailableSettings.HBM2DDL_CHARSET_NAME )
            )
            : null;

    return new JpaTargetAndSourceDescriptor() {
        @Override
        public EnumSet<TargetType> getTargetTypes() {
            return EnumSet.of( TargetType.DATABASE );
        }

        @Override
        public ScriptTargetOutput getScriptTargetOutput() {
            return null;
        }

        @Override
        public SourceType getSourceType() {
            return sourceType;
        }

        @Override
        public ScriptSourceInput getScriptSourceInput() {
            return scriptSourceInput;
        }
    };
}
```

getSchemaMigrator (HibernateSchemaManagementTool.java)

```java
private static final Logger log = Logger.getLogger( HibernateSchemaManagementTool.class );

private ServiceRegistry serviceRegistry;
private GenerationTarget customTarget;

@Override
public SchemaMigrator getSchemaMigrator(Map<String,Object> options) {
    if ( determineJdbcMetadaAccessStrategy( options ) == JdbcMetadaAccessStrategy.GROUPED ) {
        return new GroupedSchemaMigratorImpl( this, getSchemaFilterProvider( options ).getMigrateFilter() );
    }
    else {
        return new IndividuallySchemaMigratorImpl( this, getSchemaFilterProvider( options ).getMigrateFilter() );
    }
}
```

doMigrate (AbstractSchemaMigrator.java) : 
```java
@Override // SchemaMigrator.java
public void doMigration(
        Metadata metadata, //변경할 스키마
        ExecutionOptions options, //변경할 실행 옵션
        ContributableMatcher contributableInclusionFilter, //사용한 contributable인스턴스에 대한 필터
        TargetDescriptor targetDescriptor) {// 변경 명령 대상에 대한 설명
    SqlStringGenerationContext sqlStringGenerationContext = SqlStringGenerationContextImpl.fromConfigurationMap(
            tool.getServiceRegistry().getService( JdbcEnvironment.class ),
            metadata.getDatabase(),
            options.getConfigurationValues()
    );
    if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
        final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
        final DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext );
        try {
            final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
                    tool.getServiceRegistry(),
                    ddlTransactionIsolator,
                    sqlStringGenerationContext,
                    tool
            );

            final GenerationTarget[] targets = tool.buildGenerationTargets(
                    targetDescriptor,
                    ddlTransactionIsolator,
                    options.getConfigurationValues()
            );

            try {
                for ( GenerationTarget target : targets ) {
                    target.prepare();
                }

                try {
                    performMigration( metadata, databaseInformation, options, contributableInclusionFilter, jdbcContext.getDialect(),
                            sqlStringGenerationContext, targets );
                }
                finally {
                    for ( GenerationTarget target : targets ) {
                        try {
                            target.release();
                        }
                        catch (Exception e) {
                            log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
                        }
                    }
                }
            }
            finally {
                try {
                    databaseInformation.cleanup();
                }
                catch (Exception e) {
                    log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
                }
            }
        }
        finally {
            ddlTransactionIsolator.release();
        }
    }
}
```
`serviceRegistry.getService({class})`: 역할별로 service를 반환한다.
script: 테이블 내 요소
database: 테이블 자체를 생성

>getSchema{auto-ddl option}(매개변수)

매개변수:
- metadata- 변경할 스키마를 나타냅니다.
- options- 변경 실행 옵션
- contributableInclusionFilter- 사용할 Contributable 인스턴스에 대한 필터
- targetDescriptor- 변경 명령 ​​대상에 대한 설명


```java
determineJdbcMetadaAccessStrategy( options ) == JdbcMetadaAccessStrategy.GROUPED
```

- GROUPED
: 매핑된 데이터베이스 테이블이 모두 있는지 확인하기 위해 단일 호출을 실행하여 모든 데이터베이스 테이블 SchemaMigrator을 검색합니다.SchemaValidatorDatabaseMetaData.getTables(String, String, String, String[])Entity


- INDIVIDUALLY
: 해당 데이터베이스 테이블이 있는지 확인하기 위해 각각에 대해 하나의 호출을 실행 합니다 SchemaMigrator.SchemaValidatorDatabaseMetaData.getTables(String, String, String, String[])Entity
