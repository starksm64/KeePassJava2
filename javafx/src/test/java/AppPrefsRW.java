import com.si.keypass.AppPrefs;
import com.si.keypass.prefs.PrefsUtils;
import io.jsondb.JsonDBTemplate;
import org.junit.Test;

/**
 * Test reading/writing the AppPrefs class
 */
public class AppPrefsRW {
    @Test
    public void testReadPrefs() throws Exception {
        JsonDBTemplate dbTemplate = PrefsUtils.getPreferences(AppPrefs.class);
        AppPrefs appPrefs = dbTemplate.findById("KeePassJava2Prefs", AppPrefs.class);
        System.out.printf("AppPrefs: %s\n", appPrefs);
    }
    @Test
    public void testWritePrefs() throws Exception {
        JsonDBTemplate dbTemplate = PrefsUtils.getPreferences(AppPrefs.class);
        //dbTemplate.createCollection(AppPrefs.class);
        AppPrefs appPrefs = new AppPrefs();
        appPrefs.setId("KeePassJava2Prefs");
        appPrefs.setYubitoolPath("/usr/local/bin/yubico-piv-tool");
        dbTemplate.upsert(appPrefs, "KeePassJava2Prefs");
    }
}
