#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class Review < ActiveRecord::Base
  belongs_to :user
  belongs_to :assignee, :class_name => "User", :foreign_key => "assignee_id"
  belongs_to :resource, :class_name => "Project", :foreign_key => "resource_id"
  belongs_to :project, :class_name => "Project", :foreign_key => "project_id"
  has_many :review_comments, :order => "created_at", :dependent => :destroy
  alias_attribute :comments, :review_comments
  belongs_to :rule_failure, :foreign_key => 'rule_failure_permanent_id', :primary_key => 'permanent_id'
  
  validates_presence_of :user, :message => "can't be empty"
  validates_presence_of :review_type, :message => "can't be empty"
  validates_presence_of :status, :message => "can't be empty"
  
  before_save :assign_project
  
  TYPE_VIOLATION = 'VIOLATION'
  TYPE_FALSE_POSITIVE = 'FALSE_POSITIVE'
  
  STATUS_OPEN = 'OPEN'
  STATUS_CLOSED = 'CLOSED'
    
  def on_project?
    resource_id==project_id
  end
  
  def rule
    @rule ||= 
      begin
        rule_failure ? rule_failure.rule : nil
      end
  end

  def self.search(options={})
    conditions=[]
    values={}
      
    review_type = options['review_type']
    if review_type
      conditions << 'review_type=:type'
      values[:type] = review_type.upcase
    else
      conditions=['review_type<>:not_type']
      values={:not_type => Review::TYPE_FALSE_POSITIVE}
    end
    
    ids=options['ids'].split(',') if options['ids']
    if options['id']
      conditions << 'id=:id'
      values[:id]=options['id'].to_i
    elsif ids && ids.size>0 && !ids[0].blank?
      conditions << 'id in (:ids)'
      values[:ids]=ids.map{|id| id.to_i}
    end

    projects=options['projects'].split(',') if options['projects']
    if projects && projects.size>0 && !projects[0].blank?
      conditions << 'project_id in (:projects)'
      projectIds = []
      projects.each do |project|
        foundProject = Project.by_key(project)
        projectIds << foundProject.id if foundProject
      end
      values[:projects]=projectIds
    end

    resources=options['resources'].split(',') if options['resources']
    if resources && resources.size>0 && !resources[0].blank?
      conditions << 'resource_id in (:resources)'
      resourceIds = []
      resources.each do |resource|
        foundResource = Project.by_key(resource)
        resourceIds << foundResource.id if foundResource
      end
      values[:resources]=resourceIds
    end

    statuses=options['statuses'].split(',') if options['statuses']
    if statuses && statuses.size>0 && !statuses[0].blank?
      conditions << 'status in (:statuses)'
      values[:statuses]=statuses
    end
    
    severities=options['severities'].split(',') if options['severities']
    if severities && severities.size>0 && !severities[0].blank?
      conditions << 'severity in (:severities)'
      values[:severities]=severities
    end

    authors=options['authors'].split(',') if options['authors']
    if authors && authors.size>0 && !authors[0].blank?
      conditions << 'user_id in (:authors)'
      unless Api::Utils.is_number?(authors[0])
        values[:authors]=User.logins_to_ids(authors)
      else
        values[:authors]=authors.map{|user_id| user_id.to_i}
      end
    end

    assignees=options['assignees'].split(',') if options['assignees']
    if assignees && assignees.size>0 && !assignees[0].blank?
      conditions << 'assignee_id in (:assignees)'
      unless Api::Utils.is_number?(assignees[0])
        values[:assignees]=User.logins_to_ids(assignees)
      else
        values[:assignees]=assignees.map{|user_id| user_id.to_i}
      end
    end

    Review.find(:all, :include => [ 'review_comments' ], :order => 'created_at DESC', :conditions => [conditions.join(' AND '), values], :limit => 200)
  end

  def self.reviews_to_xml(reviews, convert_markdown=false)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.reviews do
      reviews.each do |review|
        review.to_xml(xml, convert_markdown)
      end
    end
  end

  def to_xml(xml, convert_markdown=false)
    xml.review do
      xml.id(id.to_i)
      xml.createdAt(Api::Utils.format_datetime(created_at))
      xml.updatedAt(Api::Utils.format_datetime(updated_at))
      xml.author(user.login)
      xml.assignee(assignee.login) if assignee
      xml.title(title)
      xml.type(review_type)
      xml.status(status)
      xml.severity(severity)
      xml.resource(resource.kee)  if resource
      xml.line(resource_line) if resource_line && resource_line>0
      xml.comments do
        review_comments.each do |comment|
          xml.comment do
            xml.author(comment.user.login)
            xml.updatedAt(Api::Utils.format_datetime(comment.updated_at))
            if convert_markdown 
              xml.text(comment.html_text)
            else
              xml.text(comment.plain_text)
            end
          end
        end
      end
    end
  end

  def self.reviews_to_json(reviews, convert_markdown=false)
    JSON(reviews.collect{|review| review.to_json(convert_markdown)})
  end

  def to_json(convert_markdown=false)
    json = {}
    json['id'] = id.to_i
    json['createdAt'] = Api::Utils.format_datetime(created_at)
    json['updatedAt'] = Api::Utils.format_datetime(updated_at)
    json['author'] = user.login
    json['assignee'] = assignee.login if assignee
    json['title'] = title if title
    json['type'] = review_type
    json['status'] = status
    json['severity'] = severity
    json['resource'] = resource.kee if resource
    json['line'] = resource_line if resource_line && resource_line>0
    comments = []
    review_comments.each do |comment|
      comments << {
        'author' => comment.user.login,
        'updatedAt' => Api::Utils.format_datetime(comment.updated_at),
        'text' => convert_markdown ? comment.html_text : comment.plain_text
      }
    end
    json['comments'] = comments
    json
  end

  

  
  private
  

  
  def assign_project
    if self.project.nil? && self.resource
      self.project=self.resource.project
    end
  end
  
end
