from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
from datetime import datetime
import time
import zipfile
import os
import glob
import zipfile
import shutil
import subprocess
import signal

class QuietService(Service):
    """Custom service class that suppresses permission errors during cleanup"""
    
    def _terminate_process(self):
        """Override to suppress permission errors"""
        try:
            if self.process:
                self.process.terminate()
                self.process.wait(timeout=5)
        except (PermissionError, subprocess.TimeoutExpired, OSError):
            # Suppress these errors as they're expected in some environments
            pass
        except Exception:
            # Let other unexpected errors through
            pass


class WITSTariffScraper:
    def __init__(self, download_directory=None, headless=True):
        # Set download directory to script directory
        if download_directory is None:
            download_directory = os.path.dirname(os.path.abspath(__file__))
        self.download_directory = download_directory
        
            
        # Convert to absolute path and ensure it exists
        self.download_directory = os.path.abspath(download_directory)
        os.makedirs(self.download_directory, exist_ok=True)
        print(f"Download directory set to: {self.download_directory}")
        
        chrome_options = Options()
        if headless:
            chrome_options.add_argument('--headless')
            
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-gpu')
        chrome_options.add_argument('--remote-debugging-port=9222')
        chrome_options.add_argument('--window-size=1920,1200')
        chrome_options.add_argument('--start-maximized')
        
        # Additional options to prevent permission issues
        chrome_options.add_argument('--disable-extensions')
        chrome_options.add_argument('--disable-plugins')
        chrome_options.add_argument('--disable-images')
        chrome_options.add_argument('--disable-javascript')
        chrome_options.add_argument('--single-process')
        chrome_options.add_argument('--disable-background-networking')
        chrome_options.add_argument('--disable-default-apps')
        chrome_options.add_argument('--disable-sync')
        
        # Add user-data-dir to avoid permission issues
        chrome_options.add_argument('--user-data-dir=/tmp/chrome-user-data')
        
        # For Docker environments
        chrome_options.add_argument('--disable-background-timer-throttling')
        chrome_options.add_argument('--disable-backgrounding-occluded-windows')
        chrome_options.add_argument('--disable-renderer-backgrounding')
        
        # Configure downloads with absolute path
        prefs = {
            "download.default_directory": self.download_directory,
            "download.prompt_for_download": False,
            "download.directory_upgrade": True,
            "safebrowsing.enabled": True,
            "profile.default_content_settings.popups": 0,
            "profile.default_content_setting_values.automatic_downloads": 1
        }
        chrome_options.add_experimental_option("prefs", prefs)
        
        # Initialize service and driver with error handling
        try:
            self.service = QuietService(executable_path="chromedriver")
            self.driver = webdriver.Chrome(service=self.service, options=chrome_options)
        except Exception as e:
            print(f"Error initializing Chrome driver: {e}")
            raise
    
        self.base_url = "https://wits.worldbank.org/WITS/WITS/QuickQuery/FindTariff/FindTariff.aspx?Page=FindATariff"
        self.result_url = "https://wits.worldbank.org/WITS/WITS/Results/QueryView/QueryView.aspx?Page=DownloadandViewResults"
        
    def handle_feedback_popup(self):
        """Handle feedback popup that may appear randomly"""
        try:
            # Look for common feedback popup elements including QSI/Qualtrics
            feedback_selectors = [
                "//button[contains(text(), 'No thanks')]",
                "//button[contains(text(), 'Close')]", 
                "//button[contains(text(), 'Skip')]",
                "//button[contains(@class, 'close')]",
                "//div[contains(@class, 'modal')]//button[contains(@class, 'close')]",
                "//span[contains(@class, 'close')]",
                "//*[contains(@id, 'feedback')]//button",
                "//*[contains(@class, 'feedback')]//button",
                # QSI/Qualtrics specific selectors
                "//div[contains(@class, 'QSIWebResponsive')]//button",
                "//div[contains(@class, 'QSI')]//button[contains(text(), 'Close')]",
                "//div[contains(@class, 'QSI')]//button[contains(@class, 'close')]",
                "//div[contains(@class, 'QSIWebResponsive')]//a[contains(@class, 'close')]",
                "//button[@title='Close']",
                "//*[@id='QSIFeedbackButton-btn-close']",
                "//*[contains(@class, 'SI_')]//button",
                "//button[contains(@onclick, 'QSI')]"
            ]
            
            # Also try to hide/remove QSI containers with JavaScript
            qsi_containers = [
                ".QSIWebResponsive-creative-container-fade",
                ".QSIWebResponsive",
                "[id*='QSI']",
                "[class*='QSI']"
            ]
            
            for container in qsi_containers:
                try:
                    self.driver.execute_script(f"""
                        var elements = document.querySelectorAll('{container}');
                        for (var i = 0; i < elements.length; i++) {{
                            elements[i].style.display = 'none';
                            elements[i].remove();
                        }}
                    """)
                except:
                    pass
            
            for selector in feedback_selectors:
                try:
                    popup_element = WebDriverWait(self.driver, 1).until(
                        EC.element_to_be_clickable((By.XPATH, selector))
                    )
                    popup_element.click()
                    print("Closed feedback popup")
                    time.sleep(1)
                    return True
                except:
                    continue
                    
            return False
            
        except Exception as e:
            print(f"No feedback popup to handle: {e}")
            return False

    def safe_click(self, locator):
        """Safe click that automatically handles scrolling and feedback popups"""
        # First check for feedback popup
        self.handle_feedback_popup()
        
        try:
            # First try to click without scrolling
            element = WebDriverWait(self.driver, 5).until(
                EC.element_to_be_clickable(locator)
            )
            element.click()
            return True
            
        except Exception as first_attempt:
            # Check for feedback popup again
            self.handle_feedback_popup()
            
            try:
                # If first attempt fails, try with scrolling
                element = WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located(locator)
                )
                
                # Scroll to element
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
                time.sleep(1)
                
                # Try clicking again
                clickable_element = WebDriverWait(self.driver, 5).until(
                    EC.element_to_be_clickable(locator)
                )
                clickable_element.click()
                return True
                
            except Exception as e:
                print(f"Safe click failed even with scrolling: {e}")
                return False

    def safe_send_keys(self, locator, text):
        """Safe send keys that handles scrolling and feedback popups"""
        # First check for feedback popup
        self.handle_feedback_popup()
        
        try:
            element = WebDriverWait(self.driver, 5).until(
                EC.element_to_be_clickable(locator)
            )
            element.clear()
            element.send_keys(text)
            return True
            
        except Exception:
            # Check for feedback popup again
            self.handle_feedback_popup()
            
            try:
                # Scroll and try again
                element = WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located(locator)
                )
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
                time.sleep(1)
                
                element = WebDriverWait(self.driver, 5).until(
                    EC.element_to_be_clickable(locator)
                )
                element.clear()
                element.send_keys(text)
                return True
                
            except Exception as e:
                print(f"Safe send keys failed: {e}")
                return False
        
    def handle_alert(self, accept=True):
        """Handle alert popup"""
        try:
            alert = WebDriverWait(self.driver, 10).until(EC.alert_is_present())
            if accept:
                alert.accept()
            else:
                alert.dismiss()
            return True
        except Exception as e:
            print(f"No alert to handle: {e}")
            return False
        
    def login(self, username, password):
        """Login to WITS website"""
        try:
            # Navigate to login page
            login_url = "https://wits.worldbank.org/WITS/WITS/Restricted/Login.aspx"
            self.driver.get(login_url)
            
            # Fill login form
            self.safe_send_keys((By.NAME, "UserNameTextBox"), username)
            self.safe_send_keys((By.NAME, "UserPassTextBox"), password + Keys.ENTER)
            
            # Wait for login to complete
            WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.CLASS_NAME, "dropdown-toggle"))
            )
            
            # Check for feedback popup after login
            self.handle_feedback_popup()
            return True
            
        except Exception as e:
            print(f"Login failed: {e}")
            return False
    
    def navigate_to_tariff_page(self):
        """Navigate to the tariff query page"""
        self.driver.get(self.base_url)
        WebDriverWait(self.driver, 10).until(
            EC.presence_of_element_located((By.NAME, "ctl00$MainContent$cboDataSource"))
        )
        
        # Check for feedback popup after navigation
        time.sleep(1)
        self.handle_feedback_popup()
        
    def select_dropdown_option(self, dropdown_id, option_value):
        """Select option from dropdown by ID"""
        try:
            dropdown = Select(WebDriverWait(self.driver, 5).until(EC.element_to_be_clickable((By.ID, dropdown_id))))
            dropdown.select_by_visible_text(option_value)
            time.sleep(1)  # Wait for page to update
            return True
        except Exception as e:
            print(f"Failed to select {option_value} from {dropdown_id}: {e}")
            return False
        
    def select_radcombobox_option(self, input_id, option_text):
        """Select option from RadComboBox by typing and selecting from dropdown"""
        try:
            # Step 1: Find and clear the input field
            # Step 2: Type the option text to trigger dropdown
            self.safe_send_keys((By.ID, input_id), option_text)
            
            # Step 3: Wait for dropdown list to appear and select the option
            time.sleep(3)
            
            # Look for the dropdown item that matches the text
            # RadComboBox typically creates a list with class "rcbList"
            dropdown_option = WebDriverWait(self.driver, 5).until(
                EC.element_to_be_clickable((By.XPATH, f"//li[contains(@class, 'rcbItem') and contains(text(), '{option_text}')]"))
            )
            dropdown_option.click()
            
            return True
            
        except Exception as e:
            return False
        
    def find_and_switch_to_iframe_containing_element(self, element_locator, wait_time=2):
        """Find iframe containing specific element and switch to it"""
        try:
            time.sleep(wait_time)
            
            # First try main page
            try:
                element = WebDriverWait(self.driver, 2).until(
                    EC.presence_of_element_located(element_locator)
                )
                print("Element found in main page, no iframe switch needed")
                return False
            except:
                pass
            
            # Check each iframe
            iframes = self.driver.find_elements(By.TAG_NAME, "iframe")
            
            for i, iframe in enumerate(iframes):
                try:
                    self.driver.switch_to.frame(iframe)
                    element = WebDriverWait(self.driver, 2).until(
                        EC.presence_of_element_located(element_locator)
                    )
                    print(f"Element found in iframe {i}, switched successfully")
                    return True
                    
                except:
                    # Switch back and try next iframe
                    self.driver.switch_to.default_content()
                    continue
            
            print("Element not found in any iframe")
            return False
            
        except Exception as e:
            print(f"Error finding iframe with element: {e}")
            self.driver.switch_to.default_content()
            return False
    
    def configure_query_parameters(self, params):
        """Configure query parameters - handle both dropdowns and text inputs"""
        
        # Map parameter names to element IDs and their types
        element_mappings = {
            'datasource': {'id': 'MainContent_cboDataSource', 'type': 'dropdown'},
            'market': {'id': 'MainContent_cboMarket', 'type': 'dropdown'},
            'year': {'id': 'MainContent_cboYear', 'type': 'dropdown'},
            'dutycode': {'id': 'MainContent_cboDutyCode', 'type': 'dropdown'},
            'nomenclature': {'id': 'MainContent_cboNomenclature', 'type': 'dropdown'},
            'tier': {'id': 'MainContent_cboTier', 'type': 'dropdown'},
            'product': {'id': 'ctl00_MainContent_cboProduct_Input', 'type': 'radcombobox'},
        }
        for param, value in params.items():
            if param in element_mappings:
                element_info = element_mappings[param]
                element_id = element_info['id']
                element_type = element_info['type']
                
                print(f"Configuring {param}: {value}")
                
                # Scroll to the current element before interacting
                try:
                    if element_type == 'dropdown':
                        # Find element and scroll to it
                        element = WebDriverWait(self.driver, 10).until(
                            EC.presence_of_element_located((By.ID, element_id))
                        )
                        self.driver.execute_script("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element)
                        
                        # Now select the option
                        self.select_dropdown_option(element_id, value)
                        
                    elif element_type == 'radcombobox':
                        # Find element and scroll to it
                        element = WebDriverWait(self.driver, 10).until(
                            EC.presence_of_element_located((By.ID, element_id))
                        )
                        self.driver.execute_script("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element)
                        
                        # Now select the option
                        self.select_radcombobox_option(element_id, value)
                        
                except Exception as e:
                    print(f"Failed to configure {param}: {e}")
                    continue
                
        # Click the 'View Report' button after all parameters are set
        self.safe_click((By.ID, "MainContent_btnProceed"))
        
        return True
    
    def select_data_columns(self):
        """Select which data columns to include in the report - handle overlay loading with enhanced popup handling"""
        try:
            print("Waiting for overlay/iframe to load...")
            time.sleep(3)  # Give more time for iframe to load
            
            # Aggressively handle any popups before proceeding
            self.handle_feedback_popup()
            
            # Try to remove any overlay elements with JavaScript
            self.driver.execute_script("""
                // Remove QSI overlays
                var qsiElements = document.querySelectorAll('[class*="QSI"], [id*="QSI"]');
                for (var i = 0; i < qsiElements.length; i++) {
                    qsiElements[i].remove();
                }
                
                // Remove other common overlay classes
                var overlays = document.querySelectorAll('.overlay, .modal-backdrop, .popup-overlay');
                for (var i = 0; i < overlays.length; i++) {
                    overlays[i].remove();
                }
            """)
            
            # Switch to iframe containing column selection
            iframe_switched = self.find_and_switch_to_iframe_containing_element((By.ID, "btnMoveAll"))
            
            # Handle popups again after iframe switch
            self.handle_feedback_popup()
            
            # Retry logic for clicking Move All button
            max_attempts = 3
            for attempt in range(max_attempts):
                try:
                    print(f"Attempt {attempt + 1}: Looking for Move All button...")
                    
                    # Wait for the Move All button
                    move_all_button = WebDriverWait(self.driver, 10).until(
                        EC.presence_of_element_located((By.ID, "btnMoveAll"))
                    )
                    
                    # Scroll to button and ensure it's visible
                    self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", move_all_button)
                    time.sleep(1)
                    
                    # Try different click methods
                    try:
                        # Method 1: Regular click
                        move_all_button = WebDriverWait(self.driver, 5).until(
                            EC.element_to_be_clickable((By.ID, "btnMoveAll"))
                        )
                        move_all_button.click()
                        print("Clicked Move All button (regular click)")
                        
                    except Exception:
                        # Method 2: JavaScript click
                        print("Regular click failed, trying JavaScript click...")
                        self.driver.execute_script("document.getElementById('btnMoveAll').click();")
                        print("Clicked Move All button (JavaScript click)")
                    
                    time.sleep(2)  # Wait for the move operation to complete
                    break  # Success, exit retry loop
                    
                except Exception as e:
                    print(f"Attempt {attempt + 1} failed: {e}")
                    if attempt < max_attempts - 1:
                        # Handle popups again before retry
                        self.handle_feedback_popup()
                        time.sleep(2)
                    else:
                        raise e
            
            # Wait for and click the "Processed" button with retry logic
            for attempt in range(max_attempts):
                try:
                    processed_button = WebDriverWait(self.driver, 10).until(
                        EC.presence_of_element_located((By.ID, "RptCoulmnSelection1_btnProcessed"))
                    )
                    
                    # Scroll to button
                    self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", processed_button)
                    time.sleep(1)
                    
                    # Try clicking
                    try:
                        processed_button = WebDriverWait(self.driver, 5).until(
                            EC.element_to_be_clickable((By.ID, "RptCoulmnSelection1_btnProcessed"))
                        )
                        processed_button.click()
                        print("Clicked Processed button (regular click)")
                        
                    except Exception:
                        # JavaScript click fallback
                        self.driver.execute_script("document.getElementById('RptCoulmnSelection1_btnProcessed').click();")
                        print("Clicked Processed button (JavaScript click)")
                    
                    break  # Success
                    
                except Exception as e:
                    print(f"Processed button attempt {attempt + 1} failed: {e}")
                    if attempt < max_attempts - 1:
                        self.handle_feedback_popup()
                        time.sleep(2)
                    else:
                        raise e
            
            print("Column selection completed successfully")
            return True
            
        except Exception as e:
            print(f"Failed to select columns: {e}")
            return False
    
    def initiate_request(self):
        """Trigger the download"""
        try:
            # Find and click download/export button
            self.safe_click((By.ID, "btnDownload"))
            
            time.sleep(2)
            
            # Swap to iframe containing download options if necessary
            iframe_switched = self.find_and_switch_to_iframe_containing_element((By.ID, "txtJobName"))
            
            # Fill in job name with today's date
            job_name = f"TariffReport{datetime.now().strftime('%Y%m%d')}"
            self.safe_send_keys((By.ID, "txtJobName"), job_name)
            
            # Fill in job description
            self.safe_send_keys((By.ID, "txtJobDesc"), "Automated tariff data download")
            
            # Select file type
            filetype_dropdown = Select(WebDriverWait(self.driver, 5).until(
                EC.element_to_be_clickable((By.ID, "ddlFileFormat"))
            ))
            filetype_dropdown.select_by_visible_text("CSV")
            
            # Click download button
            self.safe_click((By.ID, "btnDownload"))
            
            # Handle the alert popup
            self.handle_alert(accept=False)
            
            # Wait a moment for any processing
            time.sleep(2)
            return True
            
        except Exception as e:
            print(f"Initiating request failed: {e}")
            return False
    
    def navigate_to_result_page(self):
        try:       
            # Head to result site
            self.driver.get(self.result_url)
            
            # Wait for page to load and aggressively handle popups
            WebDriverWait(self.driver, 15).until(
                EC.presence_of_element_located((By.TAG_NAME, "body"))
            )
            time.sleep(3)  # Give time for any overlays to appear
            
            # Aggressively remove any overlays before proceeding
            self.handle_feedback_popup()
            
            save_button = None
            # Check for Save button with retry logic
            max_attempts = 3
            for attempt in range(max_attempts):
                print(f"Attempt {attempt + 1}: Checking for Save button...")
                
                try:
                    # Remove overlays with JavaScript before each attempt
                    self.driver.execute_script("""
                        // Remove QSI overlays aggressively
                        var qsiElements = document.querySelectorAll('[class*="QSI"], [id*="QSI"]');
                        for (var i = 0; i < qsiElements.length; i++) {
                            qsiElements[i].style.display = 'none';
                            qsiElements[i].remove();
                        }
                        
                        // Remove other overlay elements
                        var overlays = document.querySelectorAll('.overlay, .modal-backdrop, .popup-overlay, [style*="z-index: 2000000000"]');
                        for (var i = 0; i < overlays.length; i++) {
                            overlays[i].style.display = 'none';
                            overlays[i].remove();
                        }
                        
                        // Remove elements with high z-index
                        var highZElements = document.querySelectorAll('[style*="z-index"]');
                        for (var i = 0; i < highZElements.length; i++) {
                            var zIndex = window.getComputedStyle(highZElements[i]).zIndex;
                            if (zIndex && parseInt(zIndex) > 1000000) {
                                highZElements[i].style.display = 'none';
                                highZElements[i].remove();
                            }
                        }
                    """)
                    
                    # Handle feedback popups again
                    self.handle_feedback_popup()
                    
                    # Now look for the download button on the new page
                    print("Looking for download button on new page...")
                    
                    # Try multiple methods to click the Download Data tab
                    download_clicked = False
                    
                    # Method 1: Regular click
                    try:
                        download_tab = WebDriverWait(self.driver, 10).until(
                            EC.element_to_be_clickable((By.XPATH, "//span[normalize-space()='Download Data']"))
                        )
                        download_tab.click()
                        download_clicked = True
                        print("Download Data tab clicked (regular click)")
                    except Exception:
                        pass
                    
                    # Method 2: JavaScript click if regular click failed
                    if not download_clicked:
                        try:
                            self.driver.execute_script("""
                                var downloadTab = document.evaluate("//span[normalize-space()='Download Data']", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                                if (downloadTab) {
                                    downloadTab.click();
                                }
                            """)
                            download_clicked = True
                            print("Download Data tab clicked (JavaScript click)")
                        except Exception:
                            pass
                    
                    # Method 3: Try alternative selector
                    if not download_clicked:
                        try:
                            download_tab = WebDriverWait(self.driver, 5).until(
                                EC.element_to_be_clickable((By.XPATH, "//span[@class='rtsTxt' and text()='Download Data']"))
                            )
                            download_tab.click()
                            download_clicked = True
                            print("Download Data tab clicked (alternative selector)")
                        except Exception:
                            pass
                    
                    if not download_clicked:
                        raise Exception("Could not click Download Data tab with any method")
                    
                    time.sleep(3)
                    
                    # Handle popups again after clicking
                    self.handle_feedback_popup()
                    
                    # Look for Save button
                    save_button = WebDriverWait(self.driver, 5).until(
                        EC.element_to_be_clickable((By.XPATH, "//td[@title='Save' and contains(@onclick, 'SaveJob')]"))
                    )
                    print("Save button found!")
                    break  # Exit loop if button found
                    
                except TimeoutException:
                    print(f"Save button not found on attempt {attempt + 1}")
                    
                    if attempt < max_attempts - 1:  # Don't wait on last attempt
                        print("Waiting 15 seconds before refreshing...")
                        time.sleep(15)
                        
                        print("Refreshing page...")
                        self.driver.refresh()
                        
                        # Wait for page to reload 
                        WebDriverWait(self.driver, 15).until(
                            EC.presence_of_element_located((By.TAG_NAME, "body"))
                        )
                        time.sleep(3)
                        
                        # Handle popups after refresh
                        self.handle_feedback_popup()
                        
                    else:
                        print("Save button not found after all attempts")
                        return False
                except Exception as e:
                    print(f"Error on attempt {attempt + 1}: {e}")
                    if attempt < max_attempts - 1:
                        time.sleep(5)
                        self.handle_feedback_popup()
                    else:
                        raise e
            
            # Click Save button if found
            try:
                save_button.click()
                print("Save button clicked (regular click)")
            except Exception:
                # Try JavaScript click as fallback
                self.driver.execute_script("arguments[0].click();", save_button)
                print("Save button clicked (JavaScript click)")
            
            # Wait a moment for any processing
            time.sleep(2)
            
            # Then click "Delete"
            delete_button = WebDriverWait(self.driver, 5).until(EC.element_to_be_clickable(
                (By.XPATH, "//td[@title='Delete' and contains(@onclick, 'DeleteJob')]")
            ))
            
            try:
                delete_button.click()
                print("Delete button clicked (regular click)")
            except Exception:
                # Try JavaScript click as fallback
                self.driver.execute_script("arguments[0].click();", delete_button)
                print("Delete button clicked (JavaScript click)")
            
            # Handle the alert popup
            self.handle_alert(accept=True)
            return True
            
        except Exception as e:
            print(f"Download failed: {e}")
            return False

    def get_csv_file_from_zip(self):
        """
        Extracts a zip file into a folder named after the zip file, finds the CSV inside, 
        moves it to current directory, and cleans up the extracted folder.
        """
        # Find the zip file in download directory (case-insensitive)
        zip_files = glob.glob(os.path.join(self.download_directory, "*.zip")) + glob.glob(os.path.join(self.download_directory, "*.ZIP"))
        
        if not zip_files:
            raise FileNotFoundError("No zip file found in download directory")
        
        zip_path = zip_files[0]
        
        # Create extraction folder name (zip filename without extension)
        extraction_folder = os.path.splitext(zip_path)[0]
        
        # Extract zip file to the named folder
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(extraction_folder)
        
        # Find the CSV file in the extracted folder
        csv_path = None
        
        for root, dirs, files in os.walk(extraction_folder):
            for file in files:
                if file.endswith('.csv') or file.endswith('.CSV'):
                    csv_path = os.path.join(root, file)
                    break
            if csv_path:
                break
        
        if not csv_path:
            # Clean up extraction folder if no CSV found
            shutil.rmtree(extraction_folder)
            raise FileNotFoundError("No CSV file found in extracted contents")
        
        # Move CSV to download directory
        csv_filename = os.path.basename(csv_path)
        final_csv_path = os.path.join(self.download_directory, csv_filename)
        shutil.move(csv_path, final_csv_path)
        
        # Remove the extraction folder and all its contents
        shutil.rmtree(extraction_folder)
        
        # Delete the original zip file
        os.remove(zip_path)
        
        return final_csv_path
    
    def scrape_tariff_data(self, username, password, query_params, data_columns):
        """Main method to perform complete scraping workflow"""
        try:
            # Login
            if not self.login(username, password):
                return False
            print("Login successful")
                
            # Navigate to tariff page
            self.navigate_to_tariff_page()
            print("Navigated to tariff query page")
            
            # Configure parameters
            if not self.configure_query_parameters(query_params):
                return False
            print("Query parameters configured")
            
            # Select data columns
            if not self.select_data_columns():
                 return False
            print("Data columns selected")
                
            # Initiate request
            if not self.initiate_request():
                return False
            print("Request initiated")
            
            # Navigate to result page
            if not self.navigate_to_result_page():
                return False
            print("Navigated to result page")
            
            # Wait for download to complete
            print("Waiting for download to complete...")
            time.sleep(30)  # Adjust this wait time as necessary
            
            # Extract CSV from downloaded zip
            csv_file = self.get_csv_file_from_zip()
            print("Tariff data download completed successfully")
            return True
            
        except Exception as e:
            print(f"Scraping failed: {e}")
            return False
        finally:
            self.cleanup()
    
    def __del__(self):
        """Cleanup method"""
        self.cleanup()

    def cleanup(self):
        """Properly close the driver with enhanced error handling"""
        try:
            if hasattr(self, 'driver') and self.driver:
                print("Starting cleanup process...")
                
                # Try to close all windows first
                try:
                    for handle in self.driver.window_handles:
                        self.driver.switch_to.window(handle)
                        self.driver.close()
                except Exception as e:
                    print(f"Warning: Error closing windows: {e}")
                
                # Try to quit the driver
                try:
                    self.driver.quit()
                    print("Driver quit successfully")
                except PermissionError as e:
                    print(f"Warning: Permission denied when terminating Chrome process: {e}")
                    print("This is usually harmless - the process will be cleaned up by the system.")
                except Exception as e:
                    print(f"Warning: Error during driver quit: {e}")
                
                # Set driver to None to prevent further operations
                self.driver = None
                
            # Clean up service if it exists
            if hasattr(self, 'service') and self.service:
                try:
                    if hasattr(self.service, 'stop'):
                        self.service.stop()
                except (PermissionError, Exception) as e:
                    print(f"Warning: Error stopping service: {e}")
                    
        except Exception as e:
            print(f"Warning: Error during cleanup: {e}")
        
        print("Cleanup process completed")

# Usage example
if __name__ == "__main__":
    # Configure your parameters
    download_directory = "~/Downloads"
    

    
    # Your login credentials
    username = "alisterchongyongxi@gmail.com"
    password = "LOLaugh!123"
    
    # Query parameters (you'll need to inspect the page for actual values)
    query_params = {
        'datasource': 'WTO-IDB',
        'market': 'United States',
        'year': '2023',
        'dutycode': 'All Duty Codes',
        'nomenclature': 'HS 2017',  # World
        'tier': 'Sub-Heading (all 6-digit HS codes)',
        'product': 'All Products'
    }
    
    # Data columns you want (inspect page for actual column IDs)
    data_columns = ['SimpleAverage', 'WeightedAverage', 'StandardDeviation']
    
    # Run the scraper 5 times as a test
    success_count = 0
    for i in range(5):
        print(f"Starting scrape iteration {i + 1}")
        scraper = WITSTariffScraper()
        success = scraper.scrape_tariff_data(username, password, query_params, data_columns)
        if success:
            print(f"Scrape iteration {i + 1} completed successfully")
            success_count += 1
        else:
            print(f"Scrape iteration {i + 1} failed")
        time.sleep(5)  # Wait between iterations

    print(f"Total successful scrapes: {success_count} out of 5")