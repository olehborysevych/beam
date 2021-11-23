/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:playground/config/theme.dart';
import 'package:playground/constants/colors.dart';
import 'package:playground/constants/font_weight.dart';
import 'package:playground/constants/sizes.dart';
import 'package:playground/modules/analytics/analytics_service.dart';
import 'package:playground/pages/playground/components/playground_feedback.dart';
import 'package:url_launcher/url_launcher.dart';

const kPrivacyPolicyNameText = 'Privacy Policy';
const kReportIssueText = 'Report issue in Jira';
const kCopyright = '© The Apache Software Foundation';

const kReportIssueLink = 'https://issues.apache.org/jira/projects/BEAM/issues/';
const kGooglePrivacyPolicyLink = 'https://policies.google.com/privacy';
const kPrivacyPolicyTextStart =
    '''Apache Beam Playground is a free, open source service to help developers learn about the Apache Beam programming model. Source code entered into Apache Beam Playground may be sent to servers running in Google Cloud Platform to be analyzed for errors/warnings, compiled to Java, Golang, and Scala, interpreted with Python and returned to the browser.

Source code entered into Apache Beam Playground may be stored, processed, and aggregated in order to improve the user experience of Apache Beam Playground and other Apache Beam tools. For example, we may use the source code to help offer better code completion suggestions. The raw source code is deleted after no more than 60 days.

Apache Beam Playground uses Google Analytics to report feature usage statistics. This data is used to help improve Apache Beam tools over time.

Learn more about Google’s ''';
const kGooglePrivacyPolicyText = 'privacy policy';
const kPrivacyPolicyTextEnd = '. We look forward to your ';
const kPrivacyPolicyFeedback = 'feedback';

const kDialogPadding = 40.0;

class PlaygroundPageFooter extends StatelessWidget {
  const PlaygroundPageFooter({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: ThemeColors.of(context).secondaryBackground,
      width: double.infinity,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          vertical: kSmSpacing,
          horizontal: kXlSpacing,
        ),
        child: Wrap(
          spacing: kXlSpacing,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const PlaygroundFeedback(),
            TextButton(
              style: TextButton.styleFrom(
                textStyle: const TextStyle(fontWeight: kNormalWeight),
              ),
              // ignore: avoid_print
              onPressed: () {
                launch(kReportIssueLink);
                AnalyticsService.get(context).trackClickReportIssue();
              },
              child: const Text(kReportIssueText),
            ),
            TextButton(
              style: TextButton.styleFrom(
                textStyle: const TextStyle(fontWeight: kNormalWeight),
              ),
              onPressed: () {
                showDialog<void>(
                  context: context,
                  builder: (BuildContext context) => AlertDialog(
                    titlePadding: const EdgeInsets.only(
                      top: kDialogPadding,
                      left: kDialogPadding,
                    ),
                    contentPadding: const EdgeInsets.all(kDialogPadding),
                    title: const Text(kPrivacyPolicyNameText),
                    content: Flexible(
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 500),
                        child: RichText(
                          text: TextSpan(children: [
                            const TextSpan(text: kPrivacyPolicyTextStart),
                            TextSpan(
                              text: kGooglePrivacyPolicyText,
                              style: const TextStyle(color: kLinkColor),
                              recognizer: TapGestureRecognizer()
                                ..onTap = () {
                                  AnalyticsService.get(context)
                                      .trackOpenLink(kGooglePrivacyPolicyLink);
                                  launch(kGooglePrivacyPolicyLink);
                                },
                            ),
                            const TextSpan(text: kPrivacyPolicyTextEnd),
                            TextSpan(
                              text: kPrivacyPolicyFeedback,
                              style: const TextStyle(color: kLinkColor),
                              recognizer: TapGestureRecognizer()
                                ..onTap = () {
                                  AnalyticsService.get(context)
                                      .trackOpenLink(kReportIssueLink);
                                  launch(kReportIssueLink);
                                },
                            ),
                            const TextSpan(text: '.'),
                          ]),
                        ),
                      ),
                    ),
                  ),
                );
              },
              child: const Text(kPrivacyPolicyNameText),
            ),
            const Text(kCopyright),
          ],
        ),
      ),
    );
  }
}
