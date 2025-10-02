package arbiter.constants;

public class CloudEventStrings {

  public static String CHANNEL_OPENED_V2 = """
    {
      "specversion": "1.0",
      "source": "/api/public/core/channels",
      "type": "ru.monitel.ck11.channel.opened.v2",
      "id": "47d8ecc7-0dd5-4c8a-b7b3-37a65f459320",
      "time": "2025-10-02T11:09:39.7381868Z",
      "subject": "pubchan--oDypUa8nOmViXmaBU4cDw"
    }
    """;

  public static String STREAM_STARTED_V2 = """
      {
        "specversion": "1.0",
        "id": "84066877-9255-4992-936a-16e159e3ead4",
        "source": "/api/public/core/events/pub-sub-hub",
        "type": "ru.monitel.ck11.events.stream-started.v2",
        "time": "2025-10-02T11:09:40.3124349Z",
        "datacontenttype": "application/json",
        "data": {
        "eventTypes": [
        "ru.monitel.ck11.rt-events.191ba073-8226-4348-bab0-d96e5bf8e5f0.v1"
            ]
        }
      }
    """;

  public static String RT_EVENTS = """
        {
          "specversion": "1.0",
          "source": "/api/public/rt-events",
          "type": "ru.monitel.ck11.rt-events.191ba073-8226-4348-bab0-d96e5bf8e5f0.v1",
          "id": "fd0a40a8-87c5-4010-bac8-b7974fd32021",
          "time": "2025-10-02T11:10:22.5042212Z",
          "subject": "75e26324-e4f5-4734-b94a-2e2ea1332c1a",
          "datacontenttype": "application/json",
          "data": {
          "id": "fd0a40a8-87c5-4010-bac8-b7974fd32021",
            "typeUid": "191ba073-8226-4348-bab0-d96e5bf8e5f0",
            "associatedWith": {
            "uid": "75e26324-e4f5-4734-b94a-2e2ea1332c1a"
          },
          "createdDateTime": "2025-10-02T11:10:22.5042212Z",
            "createdBy": {
            "uid": "0699e011-b242-4866-876f-7603b478f1f4"
          },
          "message": "Центр-Беларусь Критерий расчёта МДП с ПА: АДТН ВЛ 330 кВ Рославль – Кричев (ВЛ 439) ПАР Отключение Блока 2 Белорусской АЭС",
            "parameters": [
          {
            "type": "uuid",
            "uuid": "f13bb6f7-848a-49e4-9e78-1893e0323b1b"
          },
          {
            "type": "string",
            "string": "АДТН ВЛ 330 кВ Рославль – Кричев (ВЛ 439) ПАР Отключение Блока 2 Белорусской АЭС"
          }
              ]
          }
       }
    """;

  public static String MEASUREMENT_VALUES_DATA_V2 = """
        {
             "specversion": "1.0",
             "id": "f8cb29ab-e7fc-4fd2-9f43-ba04182db6f5",
             "source": "/api/public/measurement-values/actual",
             "type": "ru.monitel.ck11.measurement-values.data.v2",
             "time": "2025-10-02T11:09:39.9113422Z",
             "exclsubid": "mv-420",
             "rtdbtraceid": "[C26][R1603825]",
             "datacontenttype": "application/json",
             "data": {
                 "data": [
                     {
                         "uid": "431a4205-6af4-4f44-902e-fa58000aef41",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 4400
                     },
                     {
                         "uid": "c2736776-aad2-4003-b4cf-792c9d348b30",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048194,
                         "value": 5400
                     },
                     {
                         "uid": "faadf465-6d90-47ce-8f97-9ce6a67f6e09",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 2032.2586669921875
                     },
                     {
                         "uid": "1e0c7b22-e5b9-452d-a043-f2207b6c996c",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 2706.180908203125
                     },
                     {
                         "uid": "df69d232-1099-4a43-97d8-cfdb0672748d",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 4357.09228515625
                     },
                     {
                         "uid": "c37ba28d-392f-4c64-be65-6dfbd89fce6e",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 26319
                     },
                     {
                         "uid": "c7255414-fbc1-4df5-a0ae-8224809f3362",
                         "timeStamp": "2025-10-02T05:04:32.535Z",
                         "timeStamp2": "2025-10-02T05:04:32.532Z",
                         "qCode": 1879048194,
                         "value": 3
                     },
                     {
                         "uid": "bfcb262d-8dcf-4f8f-9f07-2c8eb500fdfb",
                         "timeStamp": "2025-09-10T10:47:30.632Z",
                         "timeStamp2": "2025-09-10T10:47:30.632Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "d222236f-26d5-4892-b167-0bd2ea545ee7",
                         "timeStamp": "2024-10-09T13:37:37.887Z",
                         "timeStamp2": "2024-10-09T13:37:37.887Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "705e54e1-beb9-428e-887f-c3b58a1870f8",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048192,
                         "value": 300
                     },
                     {
                         "uid": "715f53e6-7135-4eb3-84ec-cf003ead8576",
                         "timeStamp": "2024-10-15T14:02:03.019Z",
                         "timeStamp2": "2024-10-15T14:02:03.019Z",
                         "qCode": -1879048190,
                         "value": 370
                     },
                     {
                         "uid": "9696124c-00e4-4895-a55c-4d92ec7be720",
                         "timeStamp": "2024-10-09T13:38:03.417Z",
                         "timeStamp2": "2024-10-09T13:38:03.417Z",
                         "qCode": -1879048190,
                         "value": 9999
                     },
                     {
                         "uid": "2a96baa1-3f83-4cc5-aefe-f38a99c8ae7a",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "cc030395-12a0-4236-b287-ccf8964d7913",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": -670.3949584960938
                     },
                     {
                         "uid": "43dfcd86-4b7b-4a38-9fda-600b4f67d815",
                         "timeStamp": "2024-10-09T13:37:24.157Z",
                         "timeStamp2": "2024-10-09T13:37:24.157Z",
                         "qCode": -1879048190,
                         "value": 2
                     },
                     {
                         "uid": "7688c903-13ab-44ef-aef1-628042a5f022",
                         "timeStamp": "2024-10-09T13:38:20.703Z",
                         "timeStamp2": "2024-10-09T13:38:20.703Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "b5acb56e-b4c4-4085-a1e8-d898d9e9d9e0",
                         "timeStamp": "2024-12-03T14:02:11.842Z",
                         "timeStamp2": "2024-12-03T14:02:11.842Z",
                         "qCode": -1879048190,
                         "value": 6
                     },
                     {
                         "uid": "54f53a7f-3a1d-4cd3-85d1-af301d1ece96",
                         "timeStamp": "2024-10-09T13:38:28.839Z",
                         "timeStamp2": "2024-10-09T13:38:28.839Z",
                         "qCode": -1879048190,
                         "value": 60
                     },
                     {
                         "uid": "1775680f-ece4-437e-82e1-b2b3a82ec731",
                         "timeStamp": "2024-10-15T14:02:13.273Z",
                         "timeStamp2": "2024-10-15T14:02:13.273Z",
                         "qCode": -1879048190,
                         "value": 60
                     },
                     {
                         "uid": "f9bdcf66-56fd-4a8a-835e-2a25ec990b50",
                         "timeStamp": "2024-10-09T13:38:53.877Z",
                         "timeStamp2": "2024-10-09T13:38:53.877Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "6c70db70-e2de-499a-885a-2161d916ad97",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 3500
                     },
                     {
                         "uid": "2f114ae5-a9c4-4dfe-be97-12c0778e2d43",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048194,
                         "value": 4400
                     },
                     {
                         "uid": "e8ae167c-8eb4-4585-aab0-19d2e2226375",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 3163.31494140625
                     },
                     {
                         "uid": "2e03b1a8-42c3-472b-9a39-29d84c20e7c4",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 3163.31494140625
                     },
                     {
                         "uid": "192fbd14-7e88-47e1-8fc7-0e776c8f5e4c",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 4813.57421875
                     },
                     {
                         "uid": "e05cb293-2e2b-43a8-8737-35bbe19154dc",
                         "timeStamp": "2025-04-15T07:32:29.458Z",
                         "timeStamp2": "2025-04-15T07:32:29.411Z",
                         "qCode": -2147483646,
                         "value": 26319
                     },
                     {
                         "uid": "75aa3258-ac14-4a90-84a6-3467484eae29",
                         "timeStamp": "2025-10-02T09:14:02.224Z",
                         "timeStamp2": "2025-10-02T09:14:02.219Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "c91d4913-f69a-497c-8046-654d7fbe1240",
                         "timeStamp": "2025-09-10T10:47:30.632Z",
                         "timeStamp2": "2025-09-10T10:47:30.632Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "b9d8a8d7-4153-47d4-a9de-48ea038864d8",
                         "timeStamp": "2024-10-09T13:37:42.946Z",
                         "timeStamp2": "2024-10-09T13:37:42.946Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "1baa9abf-5695-480d-b137-3e5bce13d5b4",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 300
                     },
                     {
                         "uid": "e3ba71df-1dad-4d6d-88ee-596dee07863e",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "3bdb6f72-2db8-4bbd-a9a1-83744f3ac2b1",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 113.13562393188477
                     },
                     {
                         "uid": "7dcc8e1e-b1de-4c53-bb1f-13da2d329f1e",
                         "timeStamp": "2024-10-09T13:38:24.313Z",
                         "timeStamp2": "2024-10-09T13:38:24.313Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "358f0800-24fb-4718-a78e-921229b2df71",
                         "timeStamp": "2024-12-03T13:09:25.651Z",
                         "timeStamp2": "2024-12-03T13:09:25.651Z",
                         "qCode": -1879048190,
                         "value": 7000
                     },
                     {
                         "uid": "4d1b2ef5-d77d-4285-92af-f064d1c46b23",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 0
                     },
                     {
                         "uid": "62b5b02a-7adf-427b-a63e-57a4a5e582ec",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 0
                     },
                     {
                         "uid": "8f805a91-eeef-46f2-968e-ef043658f8d8",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 4893.748331014585
                     },
                     {
                         "uid": "d4bd8c04-c399-4360-8756-dc123ce70ee1",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 5547.470463524049
                     },
                     {
                         "uid": "a7ec7d66-5460-44af-abdd-9fceb8e2af5c",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 6551.259951028934
                     },
                     {
                         "uid": "834c528a-d26d-4cf5-abeb-cec9ad378b5c",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "a6fab5bf-5ae5-4dbd-af7c-29da12191c7a",
                         "timeStamp": "2025-08-26T07:10:33.499Z",
                         "timeStamp2": "2025-08-26T07:10:33.495Z",
                         "qCode": 1879048194,
                         "value": 6
                     },
                     {
                         "uid": "78b9b9da-d361-452d-85a5-16a794ac8a3c",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "254e21a8-c833-480a-b023-5325fc6fd3b0",
                         "timeStamp": "2023-05-31T06:06:12.245Z",
                         "timeStamp2": "2023-05-31T06:06:12.245Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "86dedf5c-4fac-4cb2-809e-587f1480995a",
                         "timeStamp": "2025-10-02T08:20:40.53Z",
                         "timeStamp2": "2025-10-02T08:20:40.526Z",
                         "qCode": 1879048192,
                         "value": 150
                     },
                     {
                         "uid": "ff9986f5-fabf-4f9e-944b-a7462ba1d9b2",
                         "timeStamp": "2025-07-22T14:36:43.482Z",
                         "timeStamp2": "2025-07-22T14:36:43.482Z",
                         "qCode": -1879048190,
                         "value": 495
                     },
                     {
                         "uid": "8de35bde-fb2e-4196-b05a-3232c402d1d9",
                         "timeStamp": "2023-12-27T11:18:25.274Z",
                         "timeStamp2": "2023-12-27T11:18:25.274Z",
                         "qCode": -2147483646,
                         "value": 10006
                     },
                     {
                         "uid": "9b69c485-031c-43bf-8c84-e0f009ea9244",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "a3458246-d9fc-4071-8198-5b0106145d1f",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 4579.406585693359
                     },
                     {
                         "uid": "80b8e4e9-b660-4f4f-87b1-86997465e5c9",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 2
                     },
                     {
                         "uid": "f4c51194-0ef6-474a-bf3d-6e740cef86a7",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "eaa7a2ae-3e1b-4f4e-a3a3-3d85f2f95b0d",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 9999
                     },
                     {
                         "uid": "4bbd0569-2b03-45f0-811b-ebd1f1052def",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 60
                     },
                     {
                         "uid": "c13eb753-81d0-44d4-a7f9-89e7d6907dab",
                         "timeStamp": "2025-04-25T12:13:42.553Z",
                         "timeStamp2": "2025-04-25T12:13:42.553Z",
                         "qCode": -1879048190,
                         "value": 100
                     },
                     {
                         "uid": "3907e9b3-4a7a-4cd6-93e5-74bb3013a0ab",
                         "timeStamp": "2023-10-11T12:29:00.107Z",
                         "timeStamp2": "2023-10-11T12:29:00.107Z",
                         "qCode": -1879048190,
                         "value": 1
                     },
                     {
                         "uid": "b6904d5a-316c-45ab-87f2-97135d2fa0cf",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048192,
                         "value": 5000
                     },
                     {
                         "uid": "293c4eed-da20-46a1-8a97-5b69252e1a13",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048192,
                         "value": 5900
                     },
                     {
                         "uid": "e3fab8a4-9985-4bd6-9066-ae7f70c12db3",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 5085.719309789334
                     },
                     {
                         "uid": "5176d61e-09ab-4230-b92b-77a0d79f8380",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 6032.30844931365
                     },
                     {
                         "uid": "fe2856ca-2e60-4660-9e31-825e733cc06b",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 7109.004326555403
                     },
                     {
                         "uid": "42f5cb41-88c3-448d-911f-01ef39bc7586",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "7c2a31f4-ae00-42fa-8f57-b15f8dfdbafd",
                         "timeStamp": "2025-08-07T13:27:56.694Z",
                         "timeStamp2": "2025-08-07T13:27:56.672Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "a34be95e-781d-4b4d-82f2-1ca17be89d4d",
                         "timeStamp": "2025-09-22T13:18:37.878Z",
                         "timeStamp2": "2025-09-22T13:18:37.878Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "35dd470f-a10c-4ea2-b05c-dc38fb152053",
                         "timeStamp": "2023-05-31T06:06:43.887Z",
                         "timeStamp2": "2023-05-31T06:06:43.887Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "d9c43f95-6cc6-4bf3-b1f9-9d8d03d3f019",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 150
                     },
                     {
                         "uid": "fd26ced0-1277-4213-b810-e4a389e69316",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "4ebe8a8e-764d-43a1-bd8d-a5c1e0fb350a",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 4182.4617919921875
                     },
                     {
                         "uid": "12e22937-8f5b-4aa0-a562-f2787d7a5874",
                         "timeStamp": "2025-07-22T13:25:14.551Z",
                         "timeStamp2": "2025-07-22T13:25:14.551Z",
                         "qCode": -1879048190,
                         "value": 1
                     },
                     {
                         "uid": "675765bf-ec11-4fad-99c4-779e8c9c07a0",
                         "timeStamp": "2025-07-22T13:29:01.079Z",
                         "timeStamp2": "2025-07-22T13:29:00.876Z",
                         "qCode": -2147483646,
                         "value": 4071
                     },
                     {
                         "uid": "ddf4ae19-9003-4823-9f36-ed35acf20eff",
                         "timeStamp": "2025-10-02T11:09:33.476Z",
                         "timeStamp2": "2025-10-02T11:09:33.471Z",
                         "qCode": 1879048192,
                         "value": 1060.2325134277344
                     },
                     {
                         "uid": "76832144-852a-4eb3-9865-9cb84d60f15b",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048194,
                         "value": 2000
                     },
                     {
                         "uid": "4d7ffdb7-7f87-4cfe-b573-109af20a70f8",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 2144.2418754137093
                     },
                     {
                         "uid": "b8d9c960-1b82-419a-8107-3fa4865e1725",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 2144.2418754137093
                     },
                     {
                         "uid": "7809f53f-9813-447b-b2c0-74fb386dc9bb",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 2580.8036644454455
                     },
                     {
                         "uid": "7634c387-42ee-439c-b91a-4dfe1487da6e",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "6d55d7cb-afac-4f66-841b-b575a9a8dd7a",
                         "timeStamp": "2025-09-11T01:43:16.777Z",
                         "timeStamp2": "2025-09-11T01:43:16.774Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "1a025c54-d369-4aa1-a859-c0c3890fa4c7",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "aa2f0784-fbb5-4116-8f38-f03ab0e2654b",
                         "timeStamp": "2023-05-31T06:06:48.42Z",
                         "timeStamp2": "2023-05-31T06:06:48.42Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "50ae9670-f60b-4515-a971-ac1311c106e5",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 100
                     },
                     {
                         "uid": "d9df9ffd-a589-4368-97bc-83570348fe0c",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "c33c2b8e-7ca8-46ef-a69f-a71dbc0e74ac",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 439.395751953125
                     },
                     {
                         "uid": "13044444-4e4e-4a9d-9f86-e825582ef927",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "36e654c1-8f89-4403-b7de-7ca4b9c59dde",
                         "timeStamp": "2025-07-22T13:29:01.079Z",
                         "timeStamp2": "2025-07-22T13:29:01.079Z",
                         "qCode": -2147483646,
                         "value": 1835
                     },
                     {
                         "uid": "c875ffbe-9922-419c-98eb-b6727bfe5268",
                         "timeStamp": "2025-10-02T11:09:21.608Z",
                         "timeStamp2": "2025-10-02T11:09:21.602Z",
                         "qCode": 1879048194,
                         "value": 1290
                     },
                     {
                         "uid": "007d9d7f-8dee-4ec6-b31e-f7a164291dde",
                         "timeStamp": "2025-10-02T11:09:21.608Z",
                         "timeStamp2": "2025-10-02T11:09:21.602Z",
                         "qCode": 1879048194,
                         "value": 1750
                     },
                     {
                         "uid": "1f1f7ce1-7a65-4a8e-b396-800e0ca1606a",
                         "timeStamp": "2025-08-12T05:28:59.545Z",
                         "timeStamp2": "2025-08-12T05:28:59.545Z",
                         "qCode": -2147483646,
                         "value": 1379.4149169921875
                     },
                     {
                         "uid": "6064d802-8671-4b07-b81e-a4b8a075d03d",
                         "timeStamp": "2025-08-12T05:28:59.545Z",
                         "timeStamp2": "2025-08-12T05:28:59.545Z",
                         "qCode": -2147483646,
                         "value": 1379.4149169921875
                     },
                     {
                         "uid": "fbb70fd0-122f-48ad-9684-d5505e016546",
                         "timeStamp": "2025-08-12T05:28:59.545Z",
                         "timeStamp2": "2025-08-12T05:28:59.545Z",
                         "qCode": -2147483646,
                         "value": 1714.9949951171875
                     },
                     {
                         "uid": "992fa8dd-cc89-49c6-92a7-0128463ab272",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "9c05537d-2809-4574-a84a-5a05aed4d9c2",
                         "timeStamp": "2025-10-02T11:09:21.608Z",
                         "timeStamp2": "2025-10-02T11:09:21.602Z",
                         "qCode": 1879048194,
                         "value": 3
                     },
                     {
                         "uid": "e648ce3d-4f38-465b-be90-3d984f51a19a",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "26cd8049-6bca-419f-b3b3-4af459e3126d",
                         "timeStamp": "2023-05-31T06:06:51.949Z",
                         "timeStamp2": "2023-05-31T06:06:51.949Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "78414d40-f8a7-41bc-98dd-9e2b64502d14",
                         "timeStamp": "2025-10-02T10:42:15.963Z",
                         "timeStamp2": "2025-10-02T10:42:15.955Z",
                         "qCode": 1879048194,
                         "value": 100
                     },
                     {
                         "uid": "3f5c7b2e-acfc-428a-85e8-b312816b7e57",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "eab4ebdf-006b-4c42-a7b2-8989296a6dcc",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": -232.2838897705078
                     },
                     {
                         "uid": "44c5f992-5f4f-4761-b8d7-423c4860c6b6",
                         "timeStamp": "2023-08-29T06:11:00Z",
                         "timeStamp2": "2023-08-29T06:11:00Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "e8cbbed3-4536-4791-8f0b-361a7b8e6461",
                         "timeStamp": "2025-07-22T13:29:01.079Z",
                         "timeStamp2": "2025-07-22T13:29:01.079Z",
                         "qCode": -2147483646,
                         "value": 7039
                     },
                     {
                         "uid": "f1752df3-d030-4de4-b4a1-453d80c4327f",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048192,
                         "value": 2455
                     },
                     {
                         "uid": "55523810-0506-4c85-85ad-1bfaf9baaad9",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048194,
                         "value": 3255
                     },
                     {
                         "uid": "f3b0fc6b-6f82-422e-9589-08a31e3e7b17",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 3157.7024727000053
                     },
                     {
                         "uid": "6770722f-e973-49f4-96d7-e91de066613c",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 3706.927198639255
                     },
                     {
                         "uid": "a2d04e85-68b1-4115-892a-4c8afaec2001",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 4492.651835018864
                     },
                     {
                         "uid": "e5c307aa-2df7-49d7-833a-3eeaf9a6d587",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "a9d060a5-212e-44d3-b3ae-d863837115be",
                         "timeStamp": "2025-09-08T18:39:52.459Z",
                         "timeStamp2": "2025-09-08T18:39:52.456Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "d1803306-9ebc-416c-a6aa-5b7fcdc0942b",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "79adf35a-f07a-4dd4-a3b9-91b4dba80ad3",
                         "timeStamp": "2024-05-08T10:36:55.872Z",
                         "timeStamp2": "2024-05-08T10:36:55.872Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "8c74e83f-d680-4385-b5e9-4457828fdf71",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048192,
                         "value": 200
                     },
                     {
                         "uid": "5041f6d5-4198-4b55-a1c8-45c531049fc1",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "d7f633c1-fbdc-4e46-9212-7c6929e473cb",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 1007.7764587402344
                     },
                     {
                         "uid": "3489b533-d85f-4936-b3f4-834b709689dc",
                         "timeStamp": "2024-05-08T10:37:22.985Z",
                         "timeStamp2": "2024-05-08T10:37:22.985Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "74f5e4dd-8ba4-40f8-b151-b9d4fa1a42b1",
                         "timeStamp": "2024-12-03T13:05:53.869Z",
                         "timeStamp2": "2024-12-03T13:05:53.869Z",
                         "qCode": -1879048190,
                         "value": 3000
                     },
                     {
                         "uid": "5b4a1dee-9cc2-4bbf-b19a-b0aa5bf663cc",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048194,
                         "value": 3400
                     },
                     {
                         "uid": "4f1871b3-7e30-4389-8195-d87209febfa6",
                         "timeStamp": "2025-10-02T10:05:07.513Z",
                         "timeStamp2": "2025-10-02T10:05:07.508Z",
                         "qCode": 1879048194,
                         "value": 4200
                     },
                     {
                         "uid": "b46cb72b-6966-41d9-9a66-b36b67c2de0c",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 3574.1801089373857
                     },
                     {
                         "uid": "a7c2d653-cf7d-40b3-b22d-ea08f3643a14",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 3800.2473170614126
                     },
                     {
                         "uid": "e3ed1c4f-d8a4-4aa3-90d6-7cd353e31d3b",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 4600.002867756269
                     },
                     {
                         "uid": "2430bc56-5642-408b-b18b-9d12bc04b57f",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "6654f7ee-9d27-4c8d-a0ff-ed3dfb56e38d",
                         "timeStamp": "2025-10-02T10:58:08.298Z",
                         "timeStamp2": "2025-10-02T10:58:08.294Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "8927a2db-493a-4635-80c7-b87bbc8c546f",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "50901601-240e-4532-9f1b-40e52368e8d6",
                         "timeStamp": "2024-05-08T10:37:05.991Z",
                         "timeStamp2": "2024-05-08T10:37:05.991Z",
                         "qCode": -1879048190,
                         "value": 10
                     },
                     {
                         "uid": "6914da9d-4ddd-4999-a631-d8d540f1d9c8",
                         "timeStamp": "2025-10-02T07:42:50.705Z",
                         "timeStamp2": "2025-10-02T07:42:50.681Z",
                         "qCode": 1879048194,
                         "value": 200
                     },
                     {
                         "uid": "1ac01dc8-e19a-4ea5-b248-1db3c102bdc2",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048194,
                         "value": 0
                     },
                     {
                         "uid": "037e92fe-1113-4281-b4a2-9e1987ec8e02",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 22.514129638671875
                     },
                     {
                         "uid": "73f888b5-0768-4f52-b276-cfebcd4fe397",
                         "timeStamp": "2024-05-08T10:37:27.125Z",
                         "timeStamp2": "2024-05-08T10:37:27.125Z",
                         "qCode": -1879048190,
                         "value": 0
                     },
                     {
                         "uid": "63379db3-be0d-4747-a760-e7905a150c19",
                         "timeStamp": "2025-07-25T05:54:06.116Z",
                         "timeStamp2": "2025-07-25T05:54:06.116Z",
                         "qCode": -1879048190,
                         "value": 810
                     },
                     {
                         "uid": "a3d642ec-4b9e-4e2a-9bd4-d80a8b4adf2c",
                         "timeStamp": "2025-10-02T11:09:04.061Z",
                         "timeStamp2": "2025-10-02T11:09:04.056Z",
                         "qCode": 1879048194,
                         "value": 2226.787887573242
                     },
                     {
                         "uid": "78bc3c32-f961-4cab-8c24-74a82299c751",
                         "timeStamp": "2025-10-02T08:19:07.096Z",
                         "timeStamp2": "2025-10-02T08:19:07.092Z",
                         "qCode": 1879048194,
                         "value": 3200
                     },
                     {
                         "uid": "dc6f15a8-c13e-4f51-a29f-6d7807bbafb7",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 99999
                     },
                     {
                         "uid": "02c19d7d-a033-4965-bfc6-4601ea257693",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 2430.7787796175353
                     },
                     {
                         "uid": "666c1837-85af-4b25-a1c0-e840fe5cedca",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 99999
                     },
                     {
                         "uid": "e7c3debf-35e6-41be-ad67-ce78bd72563b",
                         "timeStamp": "2025-10-02T11:09:22.094Z",
                         "timeStamp2": "2025-10-02T11:07:57Z",
                         "qCode": -2147483646,
                         "value": 223780
                     },
                     {
                         "uid": "cf6d1b39-1ddb-4c52-bee8-95d9a98db9fb",
                         "timeStamp": "2025-10-02T09:22:56.642Z",
                         "timeStamp2": "2025-10-02T09:22:56.639Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "ec879288-819e-486b-bb41-aaf698d406be",
                         "timeStamp": "2025-08-12T06:12:48.747Z",
                         "timeStamp2": "2025-08-12T06:12:48.747Z",
                         "qCode": 1879048194,
                         "value": 1
                     },
                     {
                         "uid": "14625cb8-bafe-4fb0-8885-e0d57836d0f0",
                         "qCode": 0,
                         "value": 0
                     },
                     {
                         "uid": "c105a7ce-74ae-461d-bd30-6c57b66cd401",
                         "timeStamp": "2025-10-02T10:45:23.286Z",
                         "timeStamp2": "2025-10-02T10:45:23.28Z",
                         "qCode": 1879048194,
                         "value": 50
                     },
                     {
                         "uid": "c9263137-ebc8-4427-8635-fc7942ae7bcf",
                         "timeStamp": "2025-07-29T06:31:09.095Z",
                         "timeStamp2": "2025-07-29T06:31:09.095Z",
                         "qCode": 1879048192,
                         "value": 1
                     },
                     {
                         "uid": "1522891a-7d06-4d83-bf78-e1c590b146f7",
                         "timeStamp": "2025-10-02T11:09:39.633Z",
                         "timeStamp2": "2025-10-02T11:09:39.628Z",
                         "qCode": 1879048194,
                         "value": 1851.4790649414062
                     },
                     {
                         "uid": "ee61e33a-78c2-4ec3-9c24-e4ee93a388b0",
                         "qCode": 0,
                         "value": 0
                     },
                     {
                         "uid": "ff8671f8-893b-40f4-af81-e1132b5b8743",
                         "qCode": 0,
                         "value": 0
                     }
                 ]
             }
         }
    """;
}
