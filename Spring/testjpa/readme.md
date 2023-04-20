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

### 변수 및 객체에 대한 사전설명
Metadata : ㅡ메ㅔㅑㅜㅎ


```java
private void performCreation(
        Metadata metadata,
        Dialect dialect, //sql의 방언
        ExecutionOptions options,
        ContributableMatcher contributableInclusionFilter,
        SourceDescriptor sourceDescriptor, //source의 타입
        GenerationTarget... targets) {
final SqlScriptCommandExtractor commandExtractor = tool.getServiceRegistry().getService( SqlScriptCommandExtractor.class );

final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() ); 
final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter(); //format할 형식

        switch ( sourceDescriptor.getSourceType() ) { //type을 가져옴
        case SCRIPT: {
        createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
        break;
        }
        case METADATA: {
        createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
        break;
        }
        case METADATA_THEN_SCRIPT: {
        createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
        createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
        break;
        }
        case SCRIPT_THEN_METADATA: {
        createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
        createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
        }
        }

        applyImportSources( options, commandExtractor, format, dialect, targets );
        }
```

## Cordinator에서 auto ddl의 방식을 찾아서 로직 실행
```java
private static void performDatabaseAction(
			final Action action,
			Metadata metadata,
			SchemaManagementTool tool,
			ServiceRegistry serviceRegistry,
			final ExecutionOptions executionOptions,
			ContributableMatcher contributableInclusionFilter) {

		// IMPL NOTE : JPA binds source and target info..

		switch ( action ) {
			case CREATE_ONLY: {
				//
				final JpaTargetAndSourceDescriptor createDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case CREATE:
			case CREATE_DROP: {
				final JpaTargetAndSourceDescriptor dropDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				final JpaTargetAndSourceDescriptor createDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final JpaTargetAndSourceDescriptor dropDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final JpaTargetAndSourceDescriptor migrateDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						MigrateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaMigrator( executionOptions.getConfigurationValues() ).doMigration(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						migrateDescriptor
				);
				break;
			}
			case VALIDATE: {
				tool.getSchemaValidator( executionOptions.getConfigurationValues() ).doValidation(
						metadata,
						executionOptions,
						contributableInclusionFilter
				);
				break;
			}
		}
	}
```

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

