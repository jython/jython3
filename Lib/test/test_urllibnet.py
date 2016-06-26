#!/usr/bin/env python

import unittest
from test import support

import socket
import urllib.request, urllib.parse, urllib.error
import sys
import os
import time

mimetools = support.import_module("mimetools", deprecated=True)


def _open_with_retry(func, host, *args, **kwargs):
    # Connecting to remote hosts is flaky.  Make it more robust
    # by retrying the connection several times.
    for i in range(3):
        try:
            return func(host, *args, **kwargs)
        except IOError as last_exc:
            continue
        except:
            raise
    raise last_exc


class URLTimeoutTest(unittest.TestCase):

    TIMEOUT = 10.0

    def setUp(self):
        socket.setdefaulttimeout(self.TIMEOUT)

    def tearDown(self):
        socket.setdefaulttimeout(None)

    def testURLread(self):
        f = _open_with_retry(urllib.request.urlopen, "http://www.python.org/")
        x = f.read()

class urlopenNetworkTests(unittest.TestCase):
    """Tests urllib.urlopen using the network.

    These tests are not exhaustive.  Assuming that testing using files does a
    good job overall of some of the basic interface features.  There are no
    tests exercising the optional 'data' and 'proxies' arguments.  No tests
    for transparent redirection have been written.

    setUp is not used for always constructing a connection to
    http://www.python.org/ since there a few tests that don't use that address
    and making a connection is expensive enough to warrant minimizing unneeded
    connections.

    """

    def urlopen(self, *args):
        return _open_with_retry(urllib.request.urlopen, *args)

    def test_basic(self):
        # Simple test expected to pass.
        open_url = self.urlopen("http://www.python.org/")
        for attr in ("read", "readline", "readlines", "fileno", "close",
                     "info", "geturl"):
            self.assertTrue(hasattr(open_url, attr), "object returned from "
                            "urlopen lacks the %s attribute" % attr)
        try:
            self.assertTrue(open_url.read(), "calling 'read' failed")
        finally:
            open_url.close()

    def test_readlines(self):
        # Test both readline and readlines.
        open_url = self.urlopen("http://www.python.org/")
        try:
            self.assertIsInstance(open_url.readline(), str,
                                  "readline did not return a string")
            self.assertIsInstance(open_url.readlines(), list,
                                  "readlines did not return a list")
        finally:
            open_url.close()

    def test_info(self):
        # Test 'info'.
        open_url = self.urlopen("http://www.python.org/")
        try:
            info_obj = open_url.info()
        finally:
            open_url.close()
            self.assertIsInstance(info_obj, mimetools.Message,
                                  "object returned by 'info' is not an "
                                  "instance of mimetools.Message")
            self.assertEqual(info_obj.getsubtype(), "html")

    def test_geturl(self):
        # Make sure same URL as opened is returned by geturl.
        #
        # This test has been changed from what's currently in our
        # lib-python/2.7 for Jython due to recent updates at the
        # python.org to use https; other tests can take advantate of
        # URL redirection
        URL = "https://www.python.org/"
        open_url = self.urlopen(URL)
        try:
            gotten_url = open_url.geturl()
        finally:
            open_url.close()
        self.assertEqual(gotten_url, URL)

    def test_getcode(self):
        # test getcode() with the fancy opener to get 404 error codes
        URL = "http://www.python.org/XXXinvalidXXX"
        open_url = urllib.request.FancyURLopener().open(URL)
        try:
            code = open_url.getcode()
        finally:
            open_url.close()
        self.assertEqual(code, 404)

    @unittest.skipIf(support.is_jython, "Sockets cannot be used as file descriptors")
    def test_fileno(self):
        if (sys.platform in ('win32',) or
                not hasattr(os, 'fdopen')):
            # On Windows, socket handles are not file descriptors; this
            # test can't pass on Windows.
            return
        # Make sure fd returned by fileno is valid.
        open_url = self.urlopen("http://www.python.org/")
        fd = open_url.fileno()
        FILE = os.fdopen(fd)
        try:
            self.assertTrue(FILE.read(), "reading from file created using fd "
                                      "returned by fileno failed")
        finally:
            FILE.close()

    def test_bad_address(self):
        # Make sure proper exception is raised when connecting to a bogus
        # address.
        bogus_domain = "sadflkjsasf.i.nvali.d"
        try:
            socket.gethostbyname(bogus_domain)
        except socket.gaierror:
            pass
        else:
            # This happens with some overzealous DNS providers such as OpenDNS
            self.skipTest("%r should not resolve for test to work" % bogus_domain)
        self.assertRaises(IOError,
                          # SF patch 809915:  In Sep 2003, VeriSign started
                          # highjacking invalid .com and .net addresses to
                          # boost traffic to their own site.  This test
                          # started failing then.  One hopes the .invalid
                          # domain will be spared to serve its defined
                          # purpose.
                          # urllib.urlopen, "http://www.sadflkjsasadf.com/")
                          urllib.request.urlopen, "http://sadflkjsasf.i.nvali.d/")

class urlretrieveNetworkTests(unittest.TestCase):
    """Tests urllib.urlretrieve using the network."""

    def urlretrieve(self, *args):
        return _open_with_retry(urllib.request.urlretrieve, *args)

    def test_basic(self):
        # Test basic functionality.
        file_location, info = self.urlretrieve("http://www.python.org/")
        self.assertTrue(os.path.exists(file_location), "file location returned by"
                        " urlretrieve is not a valid path")
        FILE = file(file_location)
        try:
            self.assertTrue(FILE.read(), "reading from the file location returned"
                         " by urlretrieve failed")
        finally:
            FILE.close()
            os.unlink(file_location)

    def test_specified_path(self):
        # Make sure that specifying the location of the file to write to works.
        file_location, info = self.urlretrieve("http://www.python.org/",
                                              support.TESTFN)
        self.assertEqual(file_location, support.TESTFN)
        self.assertTrue(os.path.exists(file_location))
        FILE = file(file_location)
        try:
            self.assertTrue(FILE.read(), "reading from temporary file failed")
        finally:
            FILE.close()
            os.unlink(file_location)

    def test_header(self):
        # Make sure header returned as 2nd value from urlretrieve is good.
        file_location, header = self.urlretrieve("http://www.python.org/")
        os.unlink(file_location)
        self.assertIsInstance(header, mimetools.Message,
                              "header is not an instance of mimetools.Message")

    def test_data_header(self):
        logo = "http://www.python.org/community/logos/python-logo-master-v3-TM.png"
        file_location, fileheaders = self.urlretrieve(logo)
        os.unlink(file_location)
        datevalue = fileheaders.getheader('Date')
        dateformat = '%a, %d %b %Y %H:%M:%S GMT'
        try:
            time.strptime(datevalue, dateformat)
        except ValueError:
            self.fail('Date value not in %r format', dateformat)



def test_main():
    support.requires('network')
    with support.check_py3k_warnings(
            ("urllib.urlopen.. has been removed", DeprecationWarning)):
        support.run_unittest(URLTimeoutTest,
                                  urlopenNetworkTests,
                                  urlretrieveNetworkTests)

if __name__ == "__main__":
    test_main()
