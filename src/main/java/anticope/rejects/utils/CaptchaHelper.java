package anticope.rejects.utils;

public class CaptchaHelper {
    public static boolean checkCaptchaItem(String itemName, String targetName, boolean fuzzy) {
        if (itemName == null || targetName == null) return false;

        String normalizedItem = itemName.toLowerCase().trim();
        String normalizedTarget = targetName.toLowerCase().trim();

        if (normalizedItem.equals(normalizedTarget)) return true;

        if (fuzzy) {
            if (normalizedItem.contains(normalizedTarget) || normalizedTarget.contains(normalizedItem)) return true;

            String[] itemWords = normalizedItem.split("\\s+");
            String[] targetWords = normalizedTarget.split("\\s+");

            for (String itemWord : itemWords) {
                for (String targetWord : targetWords) {
                    if (itemWord.equals(targetWord) && itemWord.length() > 2) return true;
                }
            }
        }

        return false;
    }
}
