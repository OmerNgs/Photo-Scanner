# Photo Scanner by OmerNGS — Android kaynak projesi

Bu proje Android 11 ve üzeri içindir. Fotoğraflar telefondaki seçtiğiniz klasörde kalır; uygulama ilk taramada dosya adlarını SQLite indeksine yazar. Barkod yazma, kamera ile barkod okuma ve barkod numarasını içeren QR kod üretme özellikleri vardır.

## Gereksinimler
- Android Studio (güncel kararlı sürüm)
- JDK 17 (Android Studio ile gelen JBR uygundur)
- Android telefon / emülatör, Android 11+
- İlk derlemede internet: Gradle bağımlılıklarını indirmek için. Uygulamanın kendisi tarama ve arama sırasında internet kullanmaz.

## APK oluşturma
1. ZIP dosyasını bilgisayarınıza çıkarın.
2. Android Studio > **Open** ile `PhotoScannerByOmerNGS` klasörünü açın.
3. Gradle senkronizasyonunun bitmesini bekleyin. Hata çıkarsa Android Studio'nun önerdiği Gradle/JDK ayarlarını uygulayın; JDK 17 kullanın.
4. Menüden **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçin.
5. APK genellikle `app/build/outputs/apk/debug/app-debug.apk` yolunda oluşur.
6. APK'yı USB ile telefona kopyalayın veya kendinize aktarın. Android ayarlarından kullandığınız dosya yöneticisine / tarayıcıya “Bilinmeyen uygulamaları yükle” izni verip APK'yı yükleyin.

## Telefonda ilk kullanım
1. Uygulamayı açın.
2. **Fotoğraf klasörünü seç / yeniden tara** düğmesine basın.
3. Barkod isimli ürün fotoğraflarının bulunduğu klasörü seçin ve erişim iznini onaylayın.
4. İndeksleme bitene kadar uygulamayı açık tutun. 50.000+ dosyada bu ilk işlem cihazın hızına ve depolama türüne göre zaman alabilir.
5. Barkodu yazın veya **Barkod okut** düğmesini kullanın.
6. **Bu barkodun QR kodunu göster** ile QR kodu açın.

## Desteklenen adlandırma
Örnek: `8691334.jpg`, `8691334.png`, `8691334.webp`. Uzantı büyük/küçük harf olabilir. Barkod, dosya adının son noktasından önceki kısmıdır. Alt klasörler de taranır.

## Önemli notlar
- Bu kaynak kodu bir başlangıç MVP'sidir; bu yanıtla birlikte derlenmiş APK değil, Android Studio'da derlenebilecek kaynak proje sağlanır.
- Klasör taraması sırasında uygulamayı kapatmayın. Çok büyük klasörlerde Android'in dosya sağlayıcısına göre tarama yavaş olabilir.
- Dosyaları taşırsanız/silerseniz klasörü yeniden tarayın.
- Android'in dosya seçicisiyle verilen izin, seçilen klasör ve alt klasörlerine erişim sağlar. `Android/data` gibi korumalı dizinler Android tarafından engellenebilir.
- Bu sürümde barkod tarayıcı ZXing'in yerleşik tarama ekranını açar. İlk Gradle derlemesi için internet gerekir; barkod okuma ve fotoğraf arama uygulama kurulduktan sonra çevrimdışıdır.
- Bazı görsel biçimleri (ör. HEIC/HEIF) cihazın yerel görüntü çözücüsüne bağlıdır; en güvenilir biçimler JPG, PNG ve WebP'dir.
