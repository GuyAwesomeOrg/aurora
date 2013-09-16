from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.common.action_chains import ActionChains
from random import randint
import unittest
from qa.web_tests import config
import time

class TestEditShowSnapshotAttributes(unittest.TestCase):

    def setUp(self):
        self.base_url = config.base_url
        self.verificationErrors = []
        self.accept_next_alert = True
        self.driver = webdriver.Firefox()
        self.driver.implicitly_wait(config.implicitly_wait)

    def test_edit_show_snapshot_attributes(self):
        driver = self.driver
        driver.maximize_window()
        driver.get(self.base_url + "/")
        driver.find_element_by_name("username").send_keys(config.username)
        driver.find_element_by_name("password").send_keys(config.password)
        driver.find_element_by_css_selector("input.loginSubmit").click()
        driver.find_element_by_id("launchInstance").click()

        instance_name = "Test_instance_%s" % str(randint(10, 10000))
        driver.find_element_by_id("input_instCreate_name") \
            .send_keys(instance_name)
        Move = ActionChains(driver).move_to_element(
            driver.find_element_by_xpath(
                '//*[@id="table_instanceSource"]/tbody/tr[5]/td[2]/button'))
        Move.perform()
        driver.find_element_by_xpath(
            '//*[@id="table_instanceSource"]/tbody/tr[5]/td[2]/button').click()
        Move = ActionChains(driver).move_to_element(
            driver.find_element_by_xpath(config.flavor_xpath))
        Move.perform()
        driver.find_element_by_xpath(config.flavor_xpath).click()
        driver.find_element_by_id("submit").click()
        self.assertTrue(self.is_element_present(By.LINK_TEXT, instance_name))
        driver.find_element_by_xpath('//*[text()="%s"]' % instance_name)
        while self.is_element_present(By.XPATH,
                                      '//*[text()="%s"]/..//..//*[text()="Build"]' % instance_name):
            time.sleep(5)
            driver.refresh()
        self.is_element_present(By.XPATH, '//*[text()="%s"]/..//..//*[text()="Active"]' % instance_name)
        driver.find_element_by_link_text(instance_name).click()
        driver.find_element_by_id('snapshot').click()
        driver.find_element_by_id("name").send_keys("%s_snap" % instance_name)
        driver.find_element_by_id("submit").click()

        Move = ActionChains(driver).move_to_element(
            driver.find_element_by_id("nav-instance-list-root"))
        Move.perform()
        driver.find_element_by_id("nav-image-list").click()
        self.assertTrue(self.is_element_present(By.XPATH,
                                                '//*[text()="%s_snap"]' % instance_name))
        Move = ActionChains(driver).move_to_element(
            driver.find_element_by_xpath('//*[text()="%s_snap"]/../*[1]'
                                         % instance_name))
        Move.perform()
        driver.find_element_by_xpath(
            '//*[text()="%s_snap"]/../*[1]' % instance_name).click()
        driver.find_element_by_id("edit").click()
        driver.find_element_by_id("name").clear()
        driver.find_element_by_id("name").send_keys("%s_snap_edit" % instance_name)
        driver.find_element_by_name("_action_update").click()
        driver.find_element_by_id("upButton").click()
        self.assertTrue(self.is_element_present(By.XPATH,
                                                '//*[text()="%s_snap_edit"]' % instance_name))
        #deletion snapshot
        Move = ActionChains(driver).move_to_element(
            driver.find_element_by_xpath('//*[text()="%s_snap_edit"]/../*[1]'
                                         % instance_name))
        Move.perform()
        driver.find_element_by_xpath(
        '//*[text()="%s_snap_edit"]/../*[1]' % instance_name).click()
        driver.find_element_by_id("delete").click()
        driver.find_element_by_id('btn-confirm').click()
        self.assertFalse(self.is_element_present(By.XPATH,
                                                '//*[text()="%s_snap_edit"]' % instance_name))
        Move = ActionChains(driver).move_to_element(
        driver.find_element_by_id("nav-instance-list-root"))
        Move.perform()
        driver.find_element_by_id("nav-instance-list").click()
        driver.find_element_by_xpath("//*[text()='%s']" % instance_name) \
        .click()
        driver.find_element_by_id('terminate').click()
        driver.find_element_by_id('btn-confirm').click()
        while self.is_element_present(By.XPATH,
                                  '//*[text()="%s"]/..//..//*[text()="Active"]' % instance_name):
            time.sleep(5)
            driver.refresh()
        self.assertFalse(self.is_element_present(By.LINK_TEXT, instance_name))

    def is_element_present(self, how, what):
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True

    def tearDown(self):
        self.driver.save_screenshot(config.screen_path)
        self.driver.quit()
        self.assertEqual([], self.verificationErrors)

if __name__ == "__main__":
    unittest.main()
