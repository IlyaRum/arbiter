package arbiter.constants;

public class CloudEventStrings {

  public static String MEASUREMENT_VALUES_DATA_V2 = """
{
    "specversion": "1.0",
    "id": "1ec05fbf-dce7-4f4d-873b-89a48b42874d",
    "source": "/api/public/measurement-values/actual",
    "type": "ru.monitel.ck11.measurement-values.data.v2",
    "datacontenttype": "application/json",
    "time": "2025-09-29T07:40:53.747905700Z",
    "data": {
        "data": [
            {
                "uid": "cc030395-12a0-4236-b287-ccf8964d7913",
                "timeStamp": "2025-09-29T07:40:53.751Z",
                "timeStamp2": "2025-09-29T07:40:53.747Z",
                "qCode": 1879048194,
                "value": -681.4049682617188
            },
            {
                "uid": "3bdb6f72-2db8-4bbd-a9a1-83744f3ac2b1",
                "timeStamp": "2025-09-29T07:40:53.751Z",
                "timeStamp2": "2025-09-29T07:40:53.747Z",
                "qCode": 1879048194,
                "value": 54.820411682128906
            }
        ]
    },
    "exclsubid": "mv-358",
    "rtdbtraceid": "[C24][R1552045]"
}
""";
}
