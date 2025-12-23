package arbiter.measurement;

import arbiter.data.StoreData;

public interface DataReadyCallback {
  void onDataReady(StoreData data, String unitId);
}
