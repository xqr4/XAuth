# XAuth 🔐
> Advanced Authentication Plugin for Minecraft Java Edition

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-green.svg)](https://www.minecraft.net)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net)

---

## 📖 Nedir?

XAuth, Minecraft Java Edition sunucuları için geliştirilmiş gelişmiş bir kimlik doğrulama eklentisidir. Cracked sunucularda oyuncu hesaplarını korumak için kayıt ve giriş sistemi sunar.

---

## ✨ Özellikler

- 🔑 **Kayıt / Giriş Sistemi** — `/register` ve `/login` komutları
- 🔒 **Hesap Koruması** — Giriş yapmadan hareket, sohbet ve komut kullanımı engellenir
- 🌑 **Körlük Efekti** — Giriş yapılana kadar ekran kararır
- 🛡️ **BCrypt Şifreleme** — Şifreler güvenli biçimde hashlenerek saklanır
- 💾 **SQLite Veritabanı** — Hafif, kurulum gerektirmeyen yerel veritabanı
- ⚡ **Oturum Sistemi** — Aynı IP'den bağlanan oyuncu otomatik giriş yapar
- 🤖 **Anti-Bot Koruması** — Çok hızlı giriş yapan bağlantılar otomatik atılır
- ⏱️ **Giriş Timeout** — Belirlenen sürede giriş yapılmazsa oyuncu atılır
- 🔢 **Deneme Limiti** — Hatalı şifre denemesi limitini aşan oyuncu atılır
- ⚙️ **Tam Config Desteği** — Tüm mesajlar ve ayarlar `config.yml` üzerinden düzenlenir

---

## 📋 Gereksinimler

| Gereksinim | Sürüm |
|------------|-------|
| Java | 17+ |
| Spigot / Paper | 1.20.x |
| Maven | 3.8+ (derleme için) |

---

## 🚀 Kurulum

1. [Releases](../../releases) sayfasından en son `XAuth.jar` dosyasını indir
2. Dosyayı sunucunun `plugins/` klasörüne at
3. Sunucuyu başlat — `plugins/XAuth/config.yml` otomatik oluşur
4. İstediğin ayarları düzenle ve `/xauth reload` ile yenile

---

## 🔨 Kaynaktan Derleme

```bash
git clone https://github.com/kullaniciadi/XAuth.git
cd XAuth
mvn package
```

Derlenen `.jar` dosyası `target/` klasöründe oluşur.

---

## 🎮 Komutlar

### Oyuncu Komutları

| Komut | Açıklama |
|-------|----------|
| `/register <şifre> <şifre_tekrar>` | Sunucuya kayıt ol |
| `/login <şifre>` | Sunucuya giriş yap |

### Admin Komutları

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/xauth unreg <kullanıcı>` | Oyuncunun kaydını sil | `xauth.admin` |
| `/xauth alts <kullanıcı>` | Aynı IP'den giriş yapan hesapları göster | `xauth.admin` |
| `/xauth reload` | Config'i yeniden yükle | `xauth.admin` |

> **Not:** `xauth.admin` izni varsayılan olarak OP'lara verilir.

---

## ⚙️ Yapılandırma

`plugins/XAuth/config.yml` dosyası üzerinden tüm ayarlar düzenlenebilir:

```yaml
# Oturum ayarları
session:
  enabled: true      # Aynı IP'den otomatik giriş
  timeout: 60        # Oturum süresi (dakika)

# Giriş ayarları
auth:
  max-login-attempts: 5     # Maksimum hatalı deneme
  min-password-length: 6    # Minimum şifre uzunluğu
  login-timeout: 60         # Giriş için süre (saniye)

# Anti-bot
anti-bot:
  enabled: true
  min-join-delay: 2         # İki giriş arası minimum süre (saniye)

# Tüm mesajlar özelleştirilebilir
messages:
  login-success: "&aHoş geldiniz, &e{player}&a!"
  # ...
```

Tüm seçenekler için [config.yml](src/main/resources/config.yml) dosyasına bakın.

---

## 🗄️ Veritabanı

XAuth, SQLite kullanır. Veritabanı dosyası `plugins/XAuth/xauth.db` konumunda oluşturulur. Harici kurulum gerekmez.

**Tablolar:**
- `xauth_players` — Kayıtlı oyuncular ve şifreler
- `xauth_ip_history` — IP geçmişi (alt hesap tespiti için)

---

## 📁 Proje Yapısı

```
src/main/java/com/xqr/auth/
├── XAuth.java                        # Ana sınıf
├── managers/
│   ├── AuthManager.java              # BCrypt şifre işlemleri
│   ├── ConfigManager.java            # Config yönetimi
│   ├── DatabaseManager.java          # SQLite işlemleri
│   └── SessionManager.java           # Oturum & anti-bot
├── commands/
│   ├── RegisterCommand.java          # /register
│   ├── LoginCommand.java             # /login
│   └── XAuthCommand.java             # /xauth (admin)
└── listeners/
    ├── PlayerAuthListener.java       # Giriş/çıkış olayları
    └── PlayerProtectListener.java    # Hareket/sohbet koruması
```

---

## 🤝 Katkıda Bulunma

Pull request'ler ve issue'lar memnuniyetle karşılanır!

1. Fork'la
2. Feature branch oluştur (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerini commit'le (`git commit -m 'Yeni özellik eklendi'`)
4. Branch'i push'la (`git push origin feature/yeni-ozellik`)
5. Pull Request aç

---

## 📜 Lisans

Bu proje **GNU General Public License v3.0** lisansı altında dağıtılmaktadır.

Ticari kullanım yasaktır. Değiştirip dağıtanlar da aynı lisansı kullanmak zorundadır.

Detaylar için [LICENSE](LICENSE) dosyasına bakın.

---

<p align="center">Made with ❤️ by xqr4</p>
