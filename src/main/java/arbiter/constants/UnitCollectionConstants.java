package arbiter.constants;

public class UnitCollectionConstants {

  public static final String CONFIG_KEY_OIK = "ОИК";
  public static final String CONFIG_KEY_ADDRESS = "адрес";
  public static final String CONFIG_KEY_ADDRESS_MEASUREMENT = "адрес получения измерений";
  public static final String CONFIG_KEY_ADDRESS_RT_EVENT = "адрес событий реального времени";
  public static final String CONFIG_KEY_ADDRESS_AUTH = "адрес получения токена";
  public static final String CONFIG_KEY_USER = "пользователь";
  public static final String CONFIG_KEY_PASSWORD = "пароль";
  public static final String CONFIG_KEY_DEBUG = "отладка";
  public static final String CONFIG_KEY_WRITE_ENABLE = "запись в ОИК";
  public static final String CONFIG_KEY_EVENT_UID = "изменение критерия МДП СМЗУ";
  public static final String CONFIG_KEY_WRITE_EVENT_UID = "запись критерия МДП СМЗУ";
  public static final String CONFIG_KEY_INSTANCE = "экземпляр";
  public static final String CONFIG_KEY_SKIP_CYCLE = "не проверять данные при старте 2 цикла";
  public static final String CONFIG_KEY_MINUS_HK = "вычитать НК";
  public static final String CONFIG_KEY_EVENT_DELTA = "секунд между циклом расчета и критерием";
  public static final String CONFIG_KEY_PORT = "порт";
  public static final String CONFIG_KEY_REQUEST_DELAY = "интервал опроса данных";
  public static final String CONFIG_KEY_CONNECT_ATTEMPT = "количество попыток соединения с ОИК";
  public static final String CONFIG_KEY_OIK_CONNECT_TIMEOUT = "пауза перед соединением с ОИК";
  public static final String CONFIG_KEY_WATCHDOG = "сторож";
  public static final String CONFIG_KEY_HEARTBEAT_ID = "id";
  public static final String CONFIG_KEY_HEARTBEAT_INTERVAL = "интервал";
  public static final String CONFIG_KEY_WATCHDOG_WAIT = "ждать";
  public static final String CONFIG_KEY_UNITS_ARRAY = "сечение";

  public static final String CONFIG_KEY_UNIT_NAME = "наименование";
  public static final String CONFIG_KEY_UNIT_GROUP = "группа";
  public static final String CONFIG_KEY_UNIT_DIRECTION = "направление";
  public static final String CONFIG_KEY_UNIT_ACTIVE = "в работе";
  public static final String CONFIG_KEY_UNIT_CHECK_BOTH = "проверять и МДП и АДП";
  public static final String CONFIG_KEY_UNIT_DELTA_TM = "Дельта ТИ";

  public static final String CONFIG_KEY_INFLUENCING_FACTORS = "Влияющие ТИ";
  public static final String CONFIG_KEY_INFLUENCING_FACTOR_ID = "id";
  public static final String CONFIG_KEY_INFLUENCING_FACTOR_NAME = "имя";

  public static final String CONFIG_KEY_TOPOLOGY = "топология";
  public static final String CONFIG_KEY_TOPOLOGY_ID = "id";
  public static final String CONFIG_KEY_TOPOLOGY_NAME = "имя";

  public static final String CONFIG_KEY_ELEMENTS = "ТС элементов";
  public static final String CONFIG_KEY_ELEMENT_ID = "id";
  public static final String CONFIG_KEY_ELEMENT_NAME = "имя";

  public static final String CONFIG_KEY_REPAIR_SCHEMA = "ремонтная схема";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_TV_SIGNALS = "телесигналы";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_CHECK_FORMULA = "проверка";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_GROUP = "группа";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_OPERATION = "операция";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION = "состав";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION_ID = "id";
  public static final String CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION_NAME = "имя";

  public static final String CONFIG_KEY_PARAMETERS = "исходные данные";
  public static final String CONFIG_KEY_PARAMETER_NAME = "имя";
  public static final String CONFIG_KEY_PARAMETER_ID = "id";
  public static final String CONFIG_KEY_PARAMETER_MIN = "min";
  public static final String CONFIG_KEY_PARAMETER_MAX = "max";

  public static final String CONFIG_KEY_ARPM = "АРПМ";
  public static final String CONFIG_KEY_ARPM_NAME = "имя";

  public static final String CONFIG_KEY_RESULTS = "результат";
  public static final String CONFIG_KEY_RESULT_NAME = "имя";
  public static final String CONFIG_KEY_RESULT_ID = "id";

  public static final String ARPM_PARAM_ARBITR_NOT_VALID = "Арбитр. Не пройдена достоверизация уставки";
  public static final String ARPM_PARAM_ADAPTIVE_SETPOINT_READ = "АРПМ адаптивная уставка чтение";
  public static final String ARPM_PARAM_ADAPTIVE_SETPOINT_WRITE = "АРПМ адаптивная уставка запись";
  public static final String ARPM_PARAM_DELTA = "АРПМ дельта";
  public static final String ARPM_PARAM_TZ = "АРПМ Тз";
  public static final String ARPM_PARAM_EXCEED_WRITTEN = "АРПМ превышение записанного";
  public static final String ARPM_PARAM_EXCEED_PREVIOUS = "АРПМ превышение предыдущего";
  public static final String ARPM_PARAM_STATE = "состояние АРПМ";
  public static final String ARPM_PARAM_STATE_TS = "ТС состояния АРПМ";



  private UnitCollectionConstants() {
  }
}
