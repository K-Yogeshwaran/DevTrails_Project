class AppConstants {
  // App info
  static const String appName = 'GigShield';
  static const String appVersion = '1.0.0';
  static const String appTagline = 'Parametric income protection for gig workers';
  
  // Storage keys
  static const String tokenKey = 'token';
  static const String workerIdKey = 'workerId';
  static const String adminTokenKey = 'adminToken';
  static const String zoneIdKey = 'zoneId';
  static const String rememberMeKey = 'remember_me';
  
  // Validation
  static const int minPasswordLength = 6;
  static const int phoneLength = 10;
  static const int maxPhoneLength = 10;
  
  // Trigger types
  static const List<Map<String, dynamic>> triggerTypes = [
    { 'key': 'rainfall', 'label': 'Heavy Rainfall', 'emoji': '??', 'value': 45 },
    { 'key': 'aqi', 'label': 'Hazardous AQI', 'emoji': '??', 'value': 250 },
    { 'key': 'heat', 'label': 'Extreme Heat', 'emoji': '??', 'value': 44 },
    { 'key': 'platform_downtime', 'label': 'Platform Downtime', 'emoji': '??', 'value': 60 },
    { 'key': 'curfew', 'label': 'Curfew / Section 144', 'emoji': '??', 'value': 1 },
  ];
  
  // Persona types
  static const Map<String, String> personaEmojis = {
    'food': '??',
    'grocery': '??',
    'ecommerce': '??',
  };
  
  // Policy tiers
  static const List<Map<String, dynamic>> policyTiers = [
    {
      'key': 'basic',
      'price': 49,
      'cap': 2000,
      'features': ['Up to ?2,000/week', 'All 5 disruption types', 'Auto payout', 'No claim filing'],
    },
    {
      'key': 'standard',
      'price': 89,
      'cap': 4500,
      'popular': true,
      'features': ['Up to ?4,500/week', 'All 5 disruption types', 'Auto payout', 'Priority processing'],
    },
    {
      'key': 'premium',
      'price': 149,
      'cap': 8000,
      'features': ['Up to ?8,000/week', 'All 5 disruption types', 'Auto payout', 'Highest coverage cap'],
    },
  ];
  
  // Seasons
  static const List<Map<String, String>> seasons = [
    { 'key': 'summer', 'label': '?? Summer' },
    { 'key': 'monsoon', 'label': '?? Monsoon' },
    { 'key': 'winter', 'label': '?? Winter' },
    { 'key': 'spring', 'label': '?? Spring' },
  ];
  
  // Claim statuses
  static const List<String> claimStatuses = ['all', 'approved', 'flagged', 'rejected', 'pending'];
  
  // Transaction categories
  static const Map<String, Map<String, String>> transactionCategories = {
    'initial_credit': { 'label': 'Welcome Bonus', 'icon': '??', 'color': '#22c55e' },
    'premium_debit': { 'label': 'Premium Deducted', 'icon': '??', 'color': '#f87171' },
    'auto_renewal': { 'label': 'Auto Renewal', 'icon': '??', 'color': '#f59e0b' },
    'claim_credit': { 'label': 'Claim Payout', 'icon': '??', 'color': '#22c55e' },
  };
  
  // Platform mapping
  static const Map<String, List<String>> platformMap = {
    'food': ['swiggy', 'zomato'],
    'grocery': ['zepto', 'blinkit', 'instamart', 'bigbasket'],
    'ecommerce': ['amazon', 'flipkart'],
  };
}
