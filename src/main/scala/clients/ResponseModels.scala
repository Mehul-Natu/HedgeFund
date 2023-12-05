package clients

import spray.json._


trait StockTimeSeriesDataModel

//For Intra Day 5 min window

case class MetaData6(`1. Information`: String, `2. Symbol`: String, `3. Last Refreshed`: String,
                     `4. Interval`: String, `5. Output Size`: String,
                     `6. Time Zone`: String)

case class TimeSeriesData(`1. open`: String, `2. high`: String, `3. low`: String, `4. close`: String, `5. volume`: String)

case class StockDataIntraDay1Min(`Meta Data`: MetaData6, `Time Series (1min)`: Map[String, TimeSeriesData]) extends StockTimeSeriesDataModel

//For Intra Day 15 min window
case class StockDataIntraDay15Min(`Meta Data`: MetaData6, `Time Series (15min)`: Map[String, TimeSeriesData]) extends StockTimeSeriesDataModel


//For Intra Day 60 min window
case class StockDataIntraDay60Min(`Meta Data`: MetaData6, `Time Series (60min)`: Map[String, TimeSeriesData]) extends StockTimeSeriesDataModel


//For Daily
case class MetaDataDaily(`1. Information`: String, `2. Symbol`: String, `3. Last Refreshed`: String,
                      `4. Output Size`: String,
                     `5. Time Zone`: String)

case class StockDataDaily(`Meta Data`: MetaDataDaily, `Time Series (Daily)`: Map[String, TimeSeriesData]) extends StockTimeSeriesDataModel



object StockResponseJsonProtocol extends DefaultJsonProtocol {
    implicit val timeSeriesDataFormat = DefaultJsonProtocol.jsonFormat5(TimeSeriesData)
    implicit val metaData6Format = jsonFormat6(MetaData6)
    implicit val stockDataIntraDay1MinFormat = jsonFormat2(StockDataIntraDay1Min)
    implicit val stockDataIntraDay15MinFormat = jsonFormat2(StockDataIntraDay15Min)
    implicit val stockDataIntraDay60MinFormat = jsonFormat2(StockDataIntraDay60Min)

  implicit val metaDataDailyFormat = jsonFormat5(MetaDataDaily)
  implicit val stockDataDailyFormat = jsonFormat2(StockDataDaily)

  }

object test extends App {

  // Use the Spray JSON parser to parse the JSON string
  val jsonString = "{\n    \"Meta Data\": {\n        \"1. Information\": \"Intraday (5min) open, high, low, close prices and volume\",\n        \"2. Symbol\": \"PRAA\",\n        \"3. Last Refreshed\": \"2023-12-01 16:00:00\",\n        \"4. Interval\": \"5min\",\n        \"5. Output Size\": \"Compact\",\n        \"6. Time Zone\": \"US/Eastern\"\n    },\n    \"Time Series (5min)\": {\n        \"2023-12-01 16:00:00\": {\n            \"1. open\": \"19.6550\",\n            \"2. high\": \"19.6700\",\n            \"3. low\": \"19.6550\",\n            \"4. close\": \"19.6700\",\n            \"5. volume\": \"102538\"\n        },\n        \"2023-12-01 15:55:00\": {\n            \"1. open\": \"19.5900\",\n            \"2. high\": \"19.6600\",\n            \"3. low\": \"19.5900\",\n            \"4. close\": \"19.6550\",\n            \"5. volume\": \"23938\"\n        },\n        \"2023-12-01 15:50:00\": {\n            \"1. open\": \"19.4700\",\n            \"2. high\": \"19.5900\",\n            \"3. low\": \"19.4600\",\n            \"4. close\": \"19.5900\",\n            \"5. volume\": \"12320\"\n        },\n        \"2023-12-01 15:45:00\": {\n            \"1. open\": \"19.5050\",\n            \"2. high\": \"19.5200\",\n            \"3. low\": \"19.4700\",\n            \"4. close\": \"19.4700\",\n            \"5. volume\": \"8809\"\n        },\n        \"2023-12-01 15:40:00\": {\n            \"1. open\": \"19.4200\",\n            \"2. high\": \"19.5300\",\n            \"3. low\": \"19.4200\",\n            \"4. close\": \"19.5050\",\n            \"5. volume\": \"8155\"\n        },\n        \"2023-12-01 15:35:00\": {\n            \"1. open\": \"19.4550\",\n            \"2. high\": \"19.4700\",\n            \"3. low\": \"19.4200\",\n            \"4. close\": \"19.4200\",\n            \"5. volume\": \"3864\"\n        },\n        \"2023-12-01 15:30:00\": {\n            \"1. open\": \"19.3900\",\n            \"2. high\": \"19.4300\",\n            \"3. low\": \"19.3900\",\n            \"4. close\": \"19.4300\",\n            \"5. volume\": \"5685\"\n        },\n        \"2023-12-01 15:25:00\": {\n            \"1. open\": \"19.3200\",\n            \"2. high\": \"19.3900\",\n            \"3. low\": \"19.3200\",\n            \"4. close\": \"19.3900\",\n            \"5. volume\": \"2153\"\n        },\n        \"2023-12-01 15:20:00\": {\n            \"1. open\": \"19.3100\",\n            \"2. high\": \"19.3200\",\n            \"3. low\": \"19.2600\",\n            \"4. close\": \"19.3000\",\n            \"5. volume\": \"5515\"\n        },\n        \"2023-12-01 15:15:00\": {\n            \"1. open\": \"19.3300\",\n            \"2. high\": \"19.3300\",\n            \"3. low\": \"19.2900\",\n            \"4. close\": \"19.2900\",\n            \"5. volume\": \"3025\"\n        },\n        \"2023-12-01 15:10:00\": {\n            \"1. open\": \"19.3400\",\n            \"2. high\": \"19.3500\",\n            \"3. low\": \"19.3400\",\n            \"4. close\": \"19.3400\",\n            \"5. volume\": \"3327\"\n        },\n        \"2023-12-01 15:05:00\": {\n            \"1. open\": \"19.3400\",\n            \"2. high\": \"19.3500\",\n            \"3. low\": \"19.3000\",\n            \"4. close\": \"19.3450\",\n            \"5. volume\": \"2887\"\n        },\n        \"2023-12-01 15:00:00\": {\n            \"1. open\": \"19.3100\",\n            \"2. high\": \"19.3400\",\n            \"3. low\": \"19.3100\",\n            \"4. close\": \"19.3400\",\n            \"5. volume\": \"771\"\n        },\n        \"2023-12-01 14:55:00\": {\n            \"1. open\": \"19.3700\",\n            \"2. high\": \"19.3700\",\n            \"3. low\": \"19.3100\",\n            \"4. close\": \"19.3100\",\n            \"5. volume\": \"2561\"\n        },\n        \"2023-12-01 14:50:00\": {\n            \"1. open\": \"19.3100\",\n            \"2. high\": \"19.3400\",\n            \"3. low\": \"19.3100\",\n            \"4. close\": \"19.3400\",\n            \"5. volume\": \"1305\"\n        },\n        \"2023-12-01 14:45:00\": {\n            \"1. open\": \"19.3600\",\n            \"2. high\": \"19.3600\",\n            \"3. low\": \"19.3000\",\n            \"4. close\": \"19.3100\",\n            \"5. volume\": \"4630\"\n        },\n        \"2023-12-01 14:40:00\": {\n            \"1. open\": \"19.3500\",\n            \"2. high\": \"19.4000\",\n            \"3. low\": \"19.3500\",\n            \"4. close\": \"19.3800\",\n            \"5. volume\": \"2180\"\n        },\n        \"2023-12-01 14:35:00\": {\n            \"1. open\": \"19.2600\",\n            \"2. high\": \"19.3800\",\n            \"3. low\": \"19.2600\",\n            \"4. close\": \"19.3600\",\n            \"5. volume\": \"5636\"\n        },\n        \"2023-12-01 14:30:00\": {\n            \"1. open\": \"19.2450\",\n            \"2. high\": \"19.2500\",\n            \"3. low\": \"19.2200\",\n            \"4. close\": \"19.2400\",\n            \"5. volume\": \"1465\"\n        },\n        \"2023-12-01 14:25:00\": {\n            \"1. open\": \"19.4100\",\n            \"2. high\": \"19.4100\",\n            \"3. low\": \"19.2200\",\n            \"4. close\": \"19.2800\",\n            \"5. volume\": \"4076\"\n        },\n        \"2023-12-01 14:20:00\": {\n            \"1. open\": \"19.5600\",\n            \"2. high\": \"19.5900\",\n            \"3. low\": \"19.4100\",\n            \"4. close\": \"19.4100\",\n            \"5. volume\": \"22508\"\n        },\n        \"2023-12-01 14:15:00\": {\n            \"1. open\": \"19.5700\",\n            \"2. high\": \"19.6000\",\n            \"3. low\": \"19.5600\",\n            \"4. close\": \"19.5800\",\n            \"5. volume\": \"3159\"\n        },\n        \"2023-12-01 14:10:00\": {\n            \"1. open\": \"19.5700\",\n            \"2. high\": \"19.6200\",\n            \"3. low\": \"19.5500\",\n            \"4. close\": \"19.5700\",\n            \"5. volume\": \"4630\"\n        },\n        \"2023-12-01 14:05:00\": {\n            \"1. open\": \"19.6100\",\n            \"2. high\": \"19.6190\",\n            \"3. low\": \"19.5800\",\n            \"4. close\": \"19.6190\",\n            \"5. volume\": \"1975\"\n        },\n        \"2023-12-01 14:00:00\": {\n            \"1. open\": \"19.5900\",\n            \"2. high\": \"19.6050\",\n            \"3. low\": \"19.5680\",\n            \"4. close\": \"19.6050\",\n            \"5. volume\": \"820\"\n        },\n        \"2023-12-01 13:55:00\": {\n            \"1. open\": \"19.5200\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.5200\",\n            \"4. close\": \"19.5800\",\n            \"5. volume\": \"5046\"\n        },\n        \"2023-12-01 13:50:00\": {\n            \"1. open\": \"19.4800\",\n            \"2. high\": \"19.5400\",\n            \"3. low\": \"19.4800\",\n            \"4. close\": \"19.5250\",\n            \"5. volume\": \"1108\"\n        },\n        \"2023-12-01 13:45:00\": {\n            \"1. open\": \"19.5050\",\n            \"2. high\": \"19.5200\",\n            \"3. low\": \"19.4800\",\n            \"4. close\": \"19.4900\",\n            \"5. volume\": \"1117\"\n        },\n        \"2023-12-01 13:40:00\": {\n            \"1. open\": \"19.5350\",\n            \"2. high\": \"19.5350\",\n            \"3. low\": \"19.4700\",\n            \"4. close\": \"19.4750\",\n            \"5. volume\": \"2462\"\n        },\n        \"2023-12-01 13:35:00\": {\n            \"1. open\": \"19.5800\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.5300\",\n            \"4. close\": \"19.5300\",\n            \"5. volume\": \"908\"\n        },\n        \"2023-12-01 13:30:00\": {\n            \"1. open\": \"19.5100\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.4850\",\n            \"4. close\": \"19.5100\",\n            \"5. volume\": \"2977\"\n        },\n        \"2023-12-01 13:25:00\": {\n            \"1. open\": \"19.5450\",\n            \"2. high\": \"19.5500\",\n            \"3. low\": \"19.4700\",\n            \"4. close\": \"19.5200\",\n            \"5. volume\": \"7114\"\n        },\n        \"2023-12-01 13:20:00\": {\n            \"1. open\": \"19.5400\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.5050\",\n            \"4. close\": \"19.5740\",\n            \"5. volume\": \"1447\"\n        },\n        \"2023-12-01 13:15:00\": {\n            \"1. open\": \"19.4800\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.4600\",\n            \"4. close\": \"19.4950\",\n            \"5. volume\": \"2235\"\n        },\n        \"2023-12-01 13:10:00\": {\n            \"1. open\": \"19.4800\",\n            \"2. high\": \"19.5400\",\n            \"3. low\": \"19.4400\",\n            \"4. close\": \"19.5300\",\n            \"5. volume\": \"3439\"\n        },\n        \"2023-12-01 13:05:00\": {\n            \"1. open\": \"19.5700\",\n            \"2. high\": \"19.5800\",\n            \"3. low\": \"19.4300\",\n            \"4. close\": \"19.4750\",\n            \"5. volume\": \"2754\"\n        },\n        \"2023-12-01 13:00:00\": {\n            \"1. open\": \"19.6300\",\n            \"2. high\": \"19.6400\",\n            \"3. low\": \"19.5190\",\n            \"4. close\": \"19.5190\",\n            \"5. volume\": \"1664\"\n        },\n        \"2023-12-01 12:55:00\": {\n            \"1. open\": \"19.6000\",\n            \"2. high\": \"19.6600\",\n            \"3. low\": \"19.5600\",\n            \"4. close\": \"19.6100\",\n            \"5. volume\": \"2963\"\n        },\n        \"2023-12-01 12:50:00\": {\n            \"1. open\": \"19.5800\",\n            \"2. high\": \"19.6500\",\n            \"3. low\": \"19.5550\",\n            \"4. close\": \"19.5800\",\n            \"5. volume\": \"3253\"\n        },\n        \"2023-12-01 12:45:00\": {\n            \"1. open\": \"19.6600\",\n            \"2. high\": \"19.6800\",\n            \"3. low\": \"19.6100\",\n            \"4. close\": \"19.6100\",\n            \"5. volume\": \"2969\"\n        },\n        \"2023-12-01 12:40:00\": {\n            \"1. open\": \"19.5700\",\n            \"2. high\": \"19.6300\",\n            \"3. low\": \"19.5400\",\n            \"4. close\": \"19.6000\",\n            \"5. volume\": \"1535\"\n        },\n        \"2023-12-01 12:35:00\": {\n            \"1. open\": \"19.6000\",\n            \"2. high\": \"19.6400\",\n            \"3. low\": \"19.5400\",\n            \"4. close\": \"19.5400\",\n            \"5. volume\": \"2255\"\n        },\n        \"2023-12-01 12:30:00\": {\n            \"1. open\": \"19.4900\",\n            \"2. high\": \"19.5900\",\n            \"3. low\": \"19.4900\",\n            \"4. close\": \"19.5700\",\n            \"5. volume\": \"2152\"\n        },\n        \"2023-12-01 12:25:00\": {\n            \"1. open\": \"19.4550\",\n            \"2. high\": \"19.5000\",\n            \"3. low\": \"19.4550\",\n            \"4. close\": \"19.5000\",\n            \"5. volume\": \"1357\"\n        },\n        \"2023-12-01 12:20:00\": {\n            \"1. open\": \"19.3550\",\n            \"2. high\": \"19.4900\",\n            \"3. low\": \"19.3550\",\n            \"4. close\": \"19.4550\",\n            \"5. volume\": \"2183\"\n        },\n        \"2023-12-01 12:15:00\": {\n            \"1. open\": \"19.3850\",\n            \"2. high\": \"19.4000\",\n            \"3. low\": \"19.3500\",\n            \"4. close\": \"19.3500\",\n            \"5. volume\": \"998\"\n        },\n        \"2023-12-01 12:10:00\": {\n            \"1. open\": \"19.3900\",\n            \"2. high\": \"19.3900\",\n            \"3. low\": \"19.3900\",\n            \"4. close\": \"19.3900\",\n            \"5. volume\": \"164\"\n        },\n        \"2023-12-01 12:05:00\": {\n            \"1. open\": \"19.3350\",\n            \"2. high\": \"19.4200\",\n            \"3. low\": \"19.3350\",\n            \"4. close\": \"19.4200\",\n            \"5. volume\": \"1586\"\n        },\n        \"2023-12-01 12:00:00\": {\n            \"1. open\": \"19.3400\",\n            \"2. high\": \"19.3400\",\n            \"3. low\": \"19.3400\",\n            \"4. close\": \"19.3400\",\n            \"5. volume\": \"233\"\n        },\n        \"2023-12-01 11:55:00\": {\n            \"1. open\": \"19.5050\",\n            \"2. high\": \"19.5050\",\n            \"3. low\": \"19.3800\",\n            \"4. close\": \"19.3800\",\n            \"5. volume\": \"2883\"\n        },\n        \"2023-12-01 11:50:00\": {\n            \"1. open\": \"19.4800\",\n            \"2. high\": \"19.5200\",\n            \"3. low\": \"19.4800\",\n            \"4. close\": \"19.5100\",\n            \"5. volume\": \"5360\"\n        },\n        \"2023-12-01 11:45:00\": {\n            \"1. open\": \"19.4900\",\n            \"2. high\": \"19.4900\",\n            \"3. low\": \"19.4600\",\n            \"4. close\": \"19.4600\",\n            \"5. volume\": \"1125\"\n        },\n        \"2023-12-01 11:40:00\": {\n            \"1. open\": \"19.4100\",\n            \"2. high\": \"19.4900\",\n            \"3. low\": \"19.4100\",\n            \"4. close\": \"19.4600\",\n            \"5. volume\": \"1714\"\n        },\n        \"2023-12-01 11:35:00\": {\n            \"1. open\": \"19.3700\",\n            \"2. high\": \"19.4000\",\n            \"3. low\": \"19.3400\",\n            \"4. close\": \"19.3750\",\n            \"5. volume\": \"1568\"\n        },\n        \"2023-12-01 11:30:00\": {\n            \"1. open\": \"19.2600\",\n            \"2. high\": \"19.3000\",\n            \"3. low\": \"19.2600\",\n            \"4. close\": \"19.2950\",\n            \"5. volume\": \"1090\"\n        },\n        \"2023-12-01 11:25:00\": {\n            \"1. open\": \"19.2200\",\n            \"2. high\": \"19.2800\",\n            \"3. low\": \"19.2200\",\n            \"4. close\": \"19.2800\",\n            \"5. volume\": \"2540\"\n        },\n        \"2023-12-01 11:20:00\": {\n            \"1. open\": \"19.2550\",\n            \"2. high\": \"19.2550\",\n            \"3. low\": \"19.1800\",\n            \"4. close\": \"19.1880\",\n            \"5. volume\": \"3012\"\n        },\n        \"2023-12-01 11:15:00\": {\n            \"1. open\": \"19.1900\",\n            \"2. high\": \"19.2600\",\n            \"3. low\": \"19.1900\",\n            \"4. close\": \"19.2500\",\n            \"5. volume\": \"3934\"\n        },\n        \"2023-12-01 11:10:00\": {\n            \"1. open\": \"19.0400\",\n            \"2. high\": \"19.1300\",\n            \"3. low\": \"19.0400\",\n            \"4. close\": \"19.1300\",\n            \"5. volume\": \"2421\"\n        },\n        \"2023-12-01 11:05:00\": {\n            \"1. open\": \"18.9600\",\n            \"2. high\": \"19.0100\",\n            \"3. low\": \"18.9100\",\n            \"4. close\": \"19.0100\",\n            \"5. volume\": \"5269\"\n        },\n        \"2023-12-01 11:00:00\": {\n            \"1. open\": \"18.9350\",\n            \"2. high\": \"18.9500\",\n            \"3. low\": \"18.8800\",\n            \"4. close\": \"18.9500\",\n            \"5. volume\": \"7470\"\n        },\n        \"2023-12-01 10:55:00\": {\n            \"1. open\": \"18.9500\",\n            \"2. high\": \"18.9530\",\n            \"3. low\": \"18.9500\",\n            \"4. close\": \"18.9530\",\n            \"5. volume\": \"300\"\n        },\n        \"2023-12-01 10:50:00\": {\n            \"1. open\": \"18.9400\",\n            \"2. high\": \"18.9400\",\n            \"3. low\": \"18.9400\",\n            \"4. close\": \"18.9400\",\n            \"5. volume\": \"447\"\n        },\n        \"2023-12-01 10:45:00\": {\n            \"1. open\": \"18.9400\",\n            \"2. high\": \"18.9700\",\n            \"3. low\": \"18.9300\",\n            \"4. close\": \"18.9400\",\n            \"5. volume\": \"3182\"\n        },\n        \"2023-12-01 10:40:00\": {\n            \"1. open\": \"18.9400\",\n            \"2. high\": \"18.9400\",\n            \"3. low\": \"18.9300\",\n            \"4. close\": \"18.9300\",\n            \"5. volume\": \"638\"\n        },\n        \"2023-12-01 10:35:00\": {\n            \"1. open\": \"18.9140\",\n            \"2. high\": \"18.9800\",\n            \"3. low\": \"18.9140\",\n            \"4. close\": \"18.9750\",\n            \"5. volume\": \"1714\"\n        },\n        \"2023-12-01 10:30:00\": {\n            \"1. open\": \"18.9150\",\n            \"2. high\": \"18.9400\",\n            \"3. low\": \"18.8700\",\n            \"4. close\": \"18.9400\",\n            \"5. volume\": \"3780\"\n        },\n        \"2023-12-01 10:25:00\": {\n            \"1. open\": \"18.9400\",\n            \"2. high\": \"18.9400\",\n            \"3. low\": \"18.9400\",\n            \"4. close\": \"18.9400\",\n            \"5. volume\": \"485\"\n        },\n        \"2023-12-01 10:20:00\": {\n            \"1. open\": \"18.9000\",\n            \"2. high\": \"18.9100\",\n            \"3. low\": \"18.7800\",\n            \"4. close\": \"18.9100\",\n            \"5. volume\": \"2391\"\n        },\n        \"2023-12-01 10:15:00\": {\n            \"1. open\": \"18.7800\",\n            \"2. high\": \"18.7800\",\n            \"3. low\": \"18.7800\",\n            \"4. close\": \"18.7800\",\n            \"5. volume\": \"129\"\n        },\n        \"2023-12-01 10:10:00\": {\n            \"1. open\": \"18.8050\",\n            \"2. high\": \"18.8600\",\n            \"3. low\": \"18.8000\",\n            \"4. close\": \"18.8300\",\n            \"5. volume\": \"1040\"\n        },\n        \"2023-12-01 10:05:00\": {\n            \"1. open\": \"18.8090\",\n            \"2. high\": \"18.8090\",\n            \"3. low\": \"18.7400\",\n            \"4. close\": \"18.8050\",\n            \"5. volume\": \"1139\"\n        },\n        \"2023-12-01 10:00:00\": {\n            \"1. open\": \"18.7200\",\n            \"2. high\": \"18.7700\",\n            \"3. low\": \"18.7200\",\n            \"4. close\": \"18.7200\",\n            \"5. volume\": \"2089\"\n        },\n        \"2023-12-01 09:55:00\": {\n            \"1. open\": \"18.6600\",\n            \"2. high\": \"18.6700\",\n            \"3. low\": \"18.6600\",\n            \"4. close\": \"18.6650\",\n            \"5. volume\": \"721\"\n        },\n        \"2023-12-01 09:50:00\": {\n            \"1. open\": \"18.5100\",\n            \"2. high\": \"18.5750\",\n            \"3. low\": \"18.5050\",\n            \"4. close\": \"18.5750\",\n            \"5. volume\": \"678\"\n        },\n        \"2023-12-01 09:45:00\": {\n            \"1. open\": \"18.4100\",\n            \"2. high\": \"18.5500\",\n            \"3. low\": \"18.3600\",\n            \"4. close\": \"18.5500\",\n            \"5. volume\": \"1062\"\n        },\n        \"2023-12-01 09:40:00\": {\n            \"1. open\": \"18.4100\",\n            \"2. high\": \"18.5000\",\n            \"3. low\": \"18.3800\",\n            \"4. close\": \"18.5000\",\n            \"5. volume\": \"852\"\n        },\n        \"2023-12-01 09:35:00\": {\n            \"1. open\": \"18.4100\",\n            \"2. high\": \"18.4100\",\n            \"3. low\": \"18.4100\",\n            \"4. close\": \"18.4100\",\n            \"5. volume\": \"315\"\n        },\n        \"2023-12-01 09:30:00\": {\n            \"1. open\": \"18.5700\",\n            \"2. high\": \"18.5700\",\n            \"3. low\": \"18.2500\",\n            \"4. close\": \"18.2600\",\n            \"5. volume\": \"4636\"\n        },\n        \"2023-12-01 08:55:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.5300\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"37\"\n        },\n        \"2023-11-30 17:40:00\": {\n            \"1. open\": \"18.5500\",\n            \"2. high\": \"18.5500\",\n            \"3. low\": \"18.5400\",\n            \"4. close\": \"18.5400\",\n            \"5. volume\": \"476\"\n        },\n        \"2023-11-30 16:45:00\": {\n            \"1. open\": \"18.5310\",\n            \"2. high\": \"18.5310\",\n            \"3. low\": \"18.5310\",\n            \"4. close\": \"18.5310\",\n            \"5. volume\": \"7278\"\n        },\n        \"2023-11-30 16:25:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.5300\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"1243\"\n        },\n        \"2023-11-30 16:20:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.5300\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"979689\"\n        },\n        \"2023-11-30 16:15:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.5300\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"952\"\n        },\n        \"2023-11-30 16:10:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.5300\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"1118\"\n        },\n        \"2023-11-30 16:00:00\": {\n            \"1. open\": \"18.5600\",\n            \"2. high\": \"18.5700\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.5300\",\n            \"5. volume\": \"353572\"\n        },\n        \"2023-11-30 15:55:00\": {\n            \"1. open\": \"18.4600\",\n            \"2. high\": \"18.5800\",\n            \"3. low\": \"18.4600\",\n            \"4. close\": \"18.5600\",\n            \"5. volume\": \"29581\"\n        },\n        \"2023-11-30 15:50:00\": {\n            \"1. open\": \"18.4100\",\n            \"2. high\": \"18.4650\",\n            \"3. low\": \"18.2600\",\n            \"4. close\": \"18.4620\",\n            \"5. volume\": \"27164\"\n        },\n        \"2023-11-30 15:45:00\": {\n            \"1. open\": \"18.4900\",\n            \"2. high\": \"18.5200\",\n            \"3. low\": \"18.4200\",\n            \"4. close\": \"18.4200\",\n            \"5. volume\": \"8808\"\n        },\n        \"2023-11-30 15:40:00\": {\n            \"1. open\": \"18.5400\",\n            \"2. high\": \"18.5500\",\n            \"3. low\": \"18.4800\",\n            \"4. close\": \"18.4800\",\n            \"5. volume\": \"5727\"\n        },\n        \"2023-11-30 15:35:00\": {\n            \"1. open\": \"18.5000\",\n            \"2. high\": \"18.5400\",\n            \"3. low\": \"18.4750\",\n            \"4. close\": \"18.5400\",\n            \"5. volume\": \"3555\"\n        },\n        \"2023-11-30 15:30:00\": {\n            \"1. open\": \"18.5600\",\n            \"2. high\": \"18.5700\",\n            \"3. low\": \"18.5100\",\n            \"4. close\": \"18.5100\",\n            \"5. volume\": \"5193\"\n        },\n        \"2023-11-30 15:25:00\": {\n            \"1. open\": \"18.5300\",\n            \"2. high\": \"18.6000\",\n            \"3. low\": \"18.5300\",\n            \"4. close\": \"18.6000\",\n            \"5. volume\": \"1610\"\n        },\n        \"2023-11-30 15:20:00\": {\n            \"1. open\": \"18.5200\",\n            \"2. high\": \"18.5400\",\n            \"3. low\": \"18.4960\",\n            \"4. close\": \"18.5150\",\n            \"5. volume\": \"5579\"\n        },\n        \"2023-11-30 15:15:00\": {\n            \"1. open\": \"18.6800\",\n            \"2. high\": \"18.6800\",\n            \"3. low\": \"18.5200\",\n            \"4. close\": \"18.5200\",\n            \"5. volume\": \"7330\"\n        },\n        \"2023-11-30 15:10:00\": {\n            \"1. open\": \"18.6100\",\n            \"2. high\": \"18.6700\",\n            \"3. low\": \"18.6100\",\n            \"4. close\": \"18.6600\",\n            \"5. volume\": \"1209\"\n        },\n        \"2023-11-30 15:05:00\": {\n            \"1. open\": \"18.6100\",\n            \"2. high\": \"18.6100\",\n            \"3. low\": \"18.6100\",\n            \"4. close\": \"18.6100\",\n            \"5. volume\": \"120\"\n        },\n        \"2023-11-30 15:00:00\": {\n            \"1. open\": \"18.6100\",\n            \"2. high\": \"18.6400\",\n            \"3. low\": \"18.5900\",\n            \"4. close\": \"18.6100\",\n            \"5. volume\": \"5944\"\n        },\n        \"2023-11-30 14:50:00\": {\n            \"1. open\": \"18.6300\",\n            \"2. high\": \"18.6300\",\n            \"3. low\": \"18.6200\",\n            \"4. close\": \"18.6200\",\n            \"5. volume\": \"730\"\n        }\n    }\n}" // Replace with your actual JSON string

  import StockResponseJsonProtocol._

  val stockData = jsonString.parseJson.convertTo[StockDataIntraDay1Min]

  // Now you can access the parsed data
  println(stockData.`Meta Data`.`2. Symbol`)
  println(stockData.`Time Series (1min)`.get(stockData.`Meta Data`.`3. Last Refreshed`))
}

